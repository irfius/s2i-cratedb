FROM centos:7.6.1810

LABEL io.k8s.description="Apache Druid" \
      io.k8s.display-name="Apache Superset 0.15.1" \
      io.openshift.expose-services="4300:http,2200:http,5432:http" \
      io.openshift.tags="cratedb,java,iot" \
      maintainer="Irfius <irfius@tuta.io>" \
      io.openshift.s2i.scripts-url="image:///usr/libexec/s2i"

ENV CRATE_DB_VERSION=4.0.4 \
    LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 

WORKDIR /app
COPY .s2i/bin/ /usr/libexec/s2i

RUN rpm --import /etc/pki/rpm-gpg/* && \
    yum -y update && \
    yum -y install java-11-openjdk && \
    yum -y clean all --enablerepo='*' && \
    mkdir -p crate && \
    mkdir -p crate/data && \
    mkdir -p crate/logs && \
    chown -R 1001:1001 /app

USER 1001
EXPOSE 5432 4200 4300
CMD ["/usr/libexec/s2i/usage"]