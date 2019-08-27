/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.exceptions;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.crate.action.sql.SQLActionException;
import io.crate.auth.user.AccessControl;
import io.crate.metadata.PartitionName;
import io.crate.sql.parser.ParsingException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.common.util.concurrent.UncategorizedExecutionException;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.EngineException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.shard.IllegalIndexShardStateException;
import org.elasticsearch.index.shard.ShardNotFoundException;
import org.elasticsearch.indices.InvalidIndexNameException;
import org.elasticsearch.indices.InvalidIndexTemplateException;
import org.elasticsearch.repositories.RepositoryMissingException;
import org.elasticsearch.snapshots.InvalidSnapshotNameException;
import org.elasticsearch.snapshots.SnapshotCreationException;
import org.elasticsearch.snapshots.SnapshotMissingException;
import org.elasticsearch.transport.TransportException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class SQLExceptions {

    private static final Logger LOGGER = LogManager.getLogger(SQLExceptions.class);

    private static final Predicate<Throwable> EXCEPTIONS_TO_UNWRAP = throwable ->
        throwable instanceof TransportException ||
        throwable instanceof UncheckedExecutionException ||
        throwable instanceof CompletionException ||
        throwable instanceof UncategorizedExecutionException ||
        throwable instanceof ExecutionException;

    public static Throwable unwrap(@Nonnull Throwable t, @Nullable Predicate<Throwable> additionalUnwrapCondition) {
        int counter = 0;
        Throwable result = t;
        Predicate<Throwable> unwrapCondition = EXCEPTIONS_TO_UNWRAP;
        if (additionalUnwrapCondition != null) {
            unwrapCondition = unwrapCondition.or(additionalUnwrapCondition);
        }
        while (unwrapCondition.test(result)) {
            Throwable cause = result.getCause();
            if (cause == null) {
                return result;
            }
            if (cause == result) {
                return result;
            }
            if (counter > 10) {
                LOGGER.warn("Exception cause unwrapping ran for 10 levels. Aborting unwrap", t);
                return result;
            }
            counter++;
            result = cause;
        }
        return result;
    }

    public static Throwable unwrap(@Nonnull Throwable t) {
        return unwrap(t, null);
    }

    public static String messageOf(@Nullable Throwable t) {
        if (t == null) {
            return "Unknown";
        }
        @SuppressWarnings("all") // throwable not thrown
            Throwable unwrappedT = unwrap(t);
        return MoreObjects.firstNonNull(unwrappedT.getMessage(), unwrappedT.toString());
    }

    public static boolean isShardFailure(Throwable e) {
        e = SQLExceptions.unwrap(e);
        return e instanceof ShardNotFoundException || e instanceof IllegalIndexShardStateException;
    }

    public static Function<Throwable, Exception> forWireTransmission(AccessControl accessControl) {
        return e -> createSQLActionException(e, accessControl::ensureMaySee);
    }

    public static SQLActionException forWireTransmission(AccessControl accessControl, Throwable e) {
        return createSQLActionException(e, accessControl::ensureMaySee);
    }

    /**
     * Create a {@link SQLActionException} out of a {@link Throwable}.
     * If concrete {@link ElasticsearchException} is found, first transform it
     * to a {@link CrateException}
     */
    public static SQLActionException createSQLActionException(Throwable e, Consumer<Throwable> maskSensitiveInformation) {
        // ideally this method would be a static factory method in SQLActionException,
        // but that would pull too many dependencies for the client

        if (e instanceof SQLActionException) {
            return (SQLActionException) e;
        }
        Throwable unwrappedError = SQLExceptions.unwrap(e);
        e = esToCrateException(unwrappedError);
        try {
            maskSensitiveInformation.accept(e);
        } catch (Exception mpe) {
            e = mpe;
        }

        int errorCode = 5000;
        HttpResponseStatus httpStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof CrateException) {
            CrateException crateException = (CrateException) e;
            if (e instanceof ValidationException) {
                errorCode = 4000 + crateException.errorCode();
                httpStatus = HttpResponseStatus.BAD_REQUEST;
            } else if (e instanceof UnauthorizedException) {
                errorCode = 4010 + crateException.errorCode();
                httpStatus = HttpResponseStatus.UNAUTHORIZED;
            } else if (e instanceof ReadOnlyException) {
                errorCode = 4030 + crateException.errorCode();
                httpStatus = HttpResponseStatus.FORBIDDEN;
            } else if (e instanceof ResourceUnknownException) {
                errorCode = 4040 + crateException.errorCode();
                httpStatus = HttpResponseStatus.NOT_FOUND;
            } else if (e instanceof ConflictException) {
                errorCode = 4090 + crateException.errorCode();
                httpStatus = HttpResponseStatus.CONFLICT;
            } else if (e instanceof UnhandledServerException) {
                errorCode = 5000 + crateException.errorCode();
            }
        } else if (e instanceof ParsingException) {
            errorCode = 4000;
            httpStatus = HttpResponseStatus.BAD_REQUEST;
        } else if (e instanceof MapperParsingException) {
            errorCode = 4000;
            httpStatus = HttpResponseStatus.BAD_REQUEST;
        }

        String message = e.getMessage();
        if (message == null) {
            if (e instanceof CrateException && e.getCause() != null) {
                e = e.getCause();   // use cause because it contains a more meaningful error in most cases
            }
            StackTraceElement[] stackTraceElements = e.getStackTrace();
            if (stackTraceElements.length > 0) {
                message = String.format(Locale.ENGLISH, "%s in %s", e.getClass().getSimpleName(), stackTraceElements[0]);
            } else {
                message = "Error in " + e.getClass().getSimpleName();
            }
        } else {
            message = e.getClass().getSimpleName() + ": " + message;
        }

        StackTraceElement[] usefulStacktrace =
            e instanceof MissingPrivilegeException ? e.getStackTrace() : unwrappedError.getStackTrace();
        return new SQLActionException(message, errorCode, httpStatus, usefulStacktrace);
    }

    private static Throwable esToCrateException(Throwable unwrappedError) {
        if (unwrappedError instanceof IllegalArgumentException || unwrappedError instanceof ParsingException) {
            return new SQLParseException(unwrappedError.getMessage(), (Exception) unwrappedError);
        } else if (unwrappedError instanceof UnsupportedOperationException) {
            return new UnsupportedFeatureException(unwrappedError.getMessage(), (Exception) unwrappedError);
        } else if (isDocumentAlreadyExistsException(unwrappedError)) {
            return new DuplicateKeyException(
                ((EngineException) unwrappedError).getIndex().getName(),
                "A document with the same primary key exists already", unwrappedError);
        } else if (unwrappedError instanceof ResourceAlreadyExistsException) {
            return new RelationAlreadyExists(((ResourceAlreadyExistsException) unwrappedError).getIndex().getName(), unwrappedError);
        } else if ((unwrappedError instanceof InvalidIndexNameException)) {
            if (unwrappedError.getMessage().contains("already exists as alias")) {
                // treat an alias like a table as aliases are not officially supported
                return new RelationAlreadyExists(((InvalidIndexNameException) unwrappedError).getIndex().getName(),
                    unwrappedError);
            }
            return new InvalidRelationName(((InvalidIndexNameException) unwrappedError).getIndex().getName(), unwrappedError);
        } else if (unwrappedError instanceof InvalidIndexTemplateException) {
            PartitionName partitionName = PartitionName.fromIndexOrTemplate(((InvalidIndexTemplateException) unwrappedError).name());
            return new InvalidRelationName(partitionName.relationName().fqn(), unwrappedError);
        } else if (unwrappedError instanceof IndexNotFoundException) {
            return new RelationUnknown(((IndexNotFoundException) unwrappedError).getIndex().getName(), unwrappedError);
        } else if (unwrappedError instanceof org.elasticsearch.common.breaker.CircuitBreakingException) {
            return new CircuitBreakingException(unwrappedError.getMessage());
        } else if (unwrappedError instanceof InterruptedException) {
            return new JobKilledException();
        } else if (unwrappedError instanceof RepositoryMissingException) {
            return new RepositoryUnknownException(((RepositoryMissingException) unwrappedError).repository());
        } else if (unwrappedError instanceof InvalidSnapshotNameException) {
            return new SnapshotNameInvalidException(unwrappedError.getMessage());
        } else if (unwrappedError instanceof SnapshotMissingException) {
            SnapshotMissingException snapshotException = (SnapshotMissingException) unwrappedError;
            return new SnapshotUnknownException(snapshotException.getRepositoryName(), snapshotException.getSnapshotName(), unwrappedError);
        } else if (unwrappedError instanceof SnapshotCreationException) {
            SnapshotCreationException creationException = (SnapshotCreationException) unwrappedError;
            return new SnapshotAlreadyExistsException(creationException.getRepositoryName(), creationException.getSnapshotName());
        }
        return unwrappedError;
    }

    public static boolean isDocumentAlreadyExistsException(Throwable e) {
        return e instanceof VersionConflictEngineException
                   && e.getMessage().contains("document already exists");
    }

    /**
     * Converts a possible ES exception to a Crate one and returns the message.
     * The message will not contain any information about possible nested exceptions.
     */
    public static String userFriendlyCrateExceptionTopOnly(Throwable e) {
        return esToCrateException(e).getMessage();
    }
}
