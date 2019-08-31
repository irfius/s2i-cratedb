# s2i-cratedb

docker build -t irfius/openshift-cratedb:1 .
s2i build creatdb/ irfius/openshift-cratedb:1 irfius/s2i-cratedb:4.0.4