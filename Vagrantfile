Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"

  config.vm.provider "virtualbox" do |v|
    v.memory = 512
    v.cpus = 1
  end

  # use the insecure key as we can't specify one for each box
  config.ssh.insert_key = false

  #eew this is growing 3big5shell TODO ansible!! (or at least not everything inline)

  config.vm.provision "file", source: "deps/consul", destination: "consul"
  config.vm.provision "file", source: "rabbit.repo", destination: "rabbit.repo"
  config.vm.provision "file", source: "rabbitmq.config", destination: "rabbitmq.config"
  config.vm.provision "file", source: "mongod.conf", destination: "mongod.conf"
  config.vm.provision "file", source: "mongodb-org-3.4.repo", destination: "mongodb-org-3.4.repo"
  config.vm.provision "shell", inline: "cp rabbitmq.config /etc/rabbitmq/rabbitmq.config"
  config.vm.provision "shell", inline: "cp mongod.conf /etc/"
  config.vm.provision "shell", inline: "cp rabbit.repo /etc/yum.repos.d/"
  config.vm.provision "shell", inline: "cp mongodb-org-3.4.repo /etc/yum.repos.d/"
  config.vm.provision "shell", inline: "rpm --import https://www.rabbitmq.com/rabbitmq-release-signing-key.asc"
  config.vm.provision "shell", inline: "rpm --import file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7"
  config.vm.provision "shell", inline: "yum install -y rabbitmq-server mongodb-org"
  config.vm.provision "shell", inline: "grep cluster01 /etc/hosts && sed -ir 's/.+?cluster01/172.28.128.11 cluster01/' /etc/hosts || echo '172.28.128.11 cluster01' >> /etc/hosts"
  config.vm.provision "shell", inline: "grep cluster02 /etc/hosts && sed -ir 's/.+?cluster02/172.28.128.12 cluster02/' /etc/hosts || echo '172.28.128.12 cluster02' >> /etc/hosts"
  config.vm.provision "shell", inline: "grep cluster03 /etc/hosts && sed -ir 's/.+?cluster03/172.28.128.13 cluster03/' /etc/hosts || echo '172.28.128.13 cluster03' >> /etc/hosts"
  config.vm.provision "shell", inline: "echo 'not really a secret lol' > /var/lib/rabbitmq/.erlang.cookie"
  config.vm.provision "shell", inline: "chown rabbitmq. /var/lib/rabbitmq/.erlang.cookie"
  config.vm.provision "shell", inline: "chmod 400 /var/lib/rabbitmq/.erlang.cookie"
  config.vm.provision "shell", inline: "service mongod restart"

  config.vm.define "cluster01" do |cluster01|
    cluster01.vm.network "private_network", ip: "172.28.128.11"
    cluster01.vm.provision "shell", inline: "hostname cluster01"
    cluster01.vm.provision "shell", inline: "grep HOSTNAME /etc/sysconfig/network && sed -ir 's/HOSTNAME=.+/HOSTNAME=cluster01/' /etc/sysconfig/network || echo 'HOSTNAME=cluster01' >> /etc/sysconfig/network"
    cluster01.vm.provision "shell", inline: "service rabbitmq-server restart" # in the define as provisioner ordering is outside-in and we need the hostname first...
  end

  config.vm.define "cluster02" do |cluster02|
    cluster02.vm.network "private_network", ip: "172.28.128.12"
    cluster02.vm.provision "shell", inline: "hostname cluster02"
    cluster02.vm.provision "shell", inline: "grep HOSTNAME /etc/sysconfig/network && sed -ir 's/HOSTNAME=.+/HOSTNAME=cluster02/' /etc/sysconfig/network || echo 'HOSTNAME=cluster02' >> /etc/sysconfig/network"
    cluster02.vm.provision "shell", inline: "service rabbitmq-server restart" # in the define as provisioner ordering is outside-in and we need the hostname first...
  end

  config.vm.define "cluster03" do |cluster03|
    cluster03.vm.network "private_network", ip: "172.28.128.13"
    cluster03.vm.provision "shell", inline: "hostname cluster03"
    cluster03.vm.provision "shell", inline: "grep HOSTNAME /etc/sysconfig/network && sed -ir 's/HOSTNAME=.+/HOSTNAME=cluster03/' /etc/sysconfig/network || echo 'HOSTNAME=cluster03' >> /etc/sysconfig/network"
    cluster03.vm.provision "shell", inline: "service rabbitmq-server restart" # in the define as provisioner ordering is outside-in and we need the hostname first...
    cluster03.vm.provision "shell", inline: "mongo --eval 'rs.initiate({_id:\"cluster\",members:[{_id:0,host:\"cluster01\"},{_id:1,host:\"cluster02\"},{_id:2,host:\"cluster03\"}]})'"
  end

end
