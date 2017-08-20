.PHONY: all cluster deps

all: deps cluster

cluster:
	vagrant up

deps/consul_0.9.0_linux_amd64.zip:
	wget https://releases.hashicorp.com/consul/0.9.0/consul_0.9.0_linux_amd64.zip -P deps

deps/consul: deps/consul_0.9.0_linux_amd64.zip
	unzip $< -d deps
	touch $@ # hack b/c extracting the file maintains the original timestamp and counts as ood, there is probably a more elegant solution but who remembers

deps: deps/consul
