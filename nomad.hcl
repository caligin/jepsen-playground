log_level = "DEBUG"
data_dir = "/tmp/nomad"
bind_addr = "address_here"

addresses {
  http = "0.0.0.0"
  rpc  = "address_here"
  serf = "address_here"
}

advertise {
  http = "0.0.0.0"
  rpc  = "address_here"
  serf = "address_here"
}

server {
    enabled = true
    bootstrap_expect = 3
}

client {
    enabled = true
  options {
    "driver.raw_exec.enable" = "1"
  }
}

