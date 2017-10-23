job "patch-then-reboot" {
  datacenters = ["dc1"]
  type = "system"

   constraint {
     attribute = "${attr.kernel.name}"
     value     = "linux"
   }

  update {
    max_parallel = 1
    min_healthy_time = "10s"
    healthy_deadline = "10m"
    auto_revert = false
    canary = 0
  }
  group "cache" {
    count = 1
    task "patch-and-reboot" {
      driver = "raw_exec"
      config {
        command = "/sbin/reboot"
      }
    }
  }
}
