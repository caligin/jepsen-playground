.PHONY: all cluster consumer deps provision test

all: deps cluster

test:
	cd jepsen.consumer/ && lein run test

cluster:
	vagrant up

provision:
	vagrant provision

deps/consul_0.9.3_linux_amd64.zip:
	wget https://releases.hashicorp.com/consul/0.9.3/consul_0.9.3_linux_amd64.zip -P deps

deps/consul: deps/consul_0.9.3_linux_amd64.zip
	unzip $< -d deps
	touch $@ # hack b/c extracting the file maintains the original timestamp and counts as ood, there is probably a more elegant solution but who remembers

consumer: demo.consumer/target/demo.consumer-0.1.0-SNAPSHOT-standalone.jar

demo.consumer/target/demo.consumer-0.1.0-SNAPSHOT-standalone.jar: $(shell find demo.consumer/src/ -type f)
	cd demo.consumer && lein uberjar

deps: deps/consul demo.consumer/target/demo.consumer-0.1.0-SNAPSHOT-standalone.jar

ensureif:
	vboxmanage list hostonlyifs | egrep 'IP|Mask' | grep -v V6 | tr -s ' ' | cut -d ' ' -f2 |  xargs -n2 ipcalc -b | grep Network | tr -s ' ' | cut -d' ' -f2 | grep $$(vboxmanage list hostonlyifs | egrep 'Mask' | grep -v V6 | tr -s ' ' | cut -d ' ' -f2 |  xargs  ipcalc -b 172.28.128.11 | grep Network | tr -s ' ' | cut -d' ' -f2) || vboxmanage hostonlyif ipconfig $$(vboxmanage hostonlyif create | grep Interface | cut -d' ' -f2 | tr -d "'") --ip 172.28.128.1
