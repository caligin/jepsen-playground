Vagrant.configure("2") do |config|
  config.vm.box = "centos/7"

  config.vm.provider "virtualbox" do |v|
    v.memory = 512
    v.cpus = 1
  end

  # use the insecure key as we can't specify one for each box
  config.ssh.insert_key = false

  config.vm.provision "file", source: "deps/consul", destination: "consul"
  config.vm.provision "file", source: "rabbit.repo", destination: "rabbit.repo"
  config.vm.provision "shell", inline: "cp rabbit.repo /etc/yum.repos.d/"
  config.vm.provision "shell", inline: "rpm --import https://www.rabbitmq.com/rabbitmq-release-signing-key.asc"
  config.vm.provision "shell", inline: "rpm --import file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7"
  config.vm.provision "shell", inline: "yum install -y rabbitmq-server"

  config.vm.define "cluster01" do |cluster|
    cluster.vm.network "private_network", ip: "172.28.128.11"
  end

  config.vm.define "cluster02" do |cluster|
    cluster.vm.network "private_network", ip: "172.28.128.12"
  end

  config.vm.define "cluster03" do |cluster|
    cluster.vm.network "private_network", ip: "172.28.128.13"
  end

end
