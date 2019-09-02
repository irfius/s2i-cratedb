FROM centos:7.6.1810

LABEL io.k8s.description="CrateDB Community 4.0.4" \
      io.k8s.display-name="CrateDB Community" \
      io.openshift.expose-services="4200:http,4300:http,5432:http" \
      io.openshift.tags="create,creatdb,java,iot" \
      maintainer="Irfius <irfius@tuta.io>" \
      io.openshift.s2i.scripts-url="image:///usr/libexec/s2i"

ENV CRATEDB_VERSION=4.0.4 \
    LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    PATH="/app/cratedb/bin:$PATH" \
    CRATE_JAVA_OPTS="-Xss500k -XX:+UnlockExperimentalVMOptions -Des.cgroups.hierarchy.override=/ " \
    CRATE_HEAP_SIZE=2g \
    CRATE_GC_LOG_DIR=/app/cratedb/data/logs \
    CRATE_HEAP_DUMP_PATH=/app/cratedb/data/data

WORKDIR /app
COPY .s2i/bin/ /usr/libexec/s2i

RUN rpm --import /etc/pki/rpm-gpg/* && \
    yum-config-manager --enable rhel-7-server-optional-rpms && \
    yum -y update && \
    yum -y install java-11-openjdk && \
    yum -y clean all --enablerepo="*" && \
    mkdir -p cratedb && \
    mkdir -p cratedb/data/logs && \
    mkdir -p cratedb/data/data && \
    mkdir -p cratedb/data/blobs && \
    chown -R 1001:1001 /app && \
    touch /etc/sysctl.conf && \
    echo "vm.max_map_count=262144" >> /etc/sysctl.conf && \
    chmod -R 1001:1001 /etc/sysctl.conf

USER 1001
EXPOSE 4200 4300 5432
CMD ["/usr/libexec/s2i/usage"]