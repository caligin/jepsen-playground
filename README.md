# clusterstuff

cluster experiments

`make ensureif cluster test` will bootstrap everything and do a test run

`make` downloads dependencies, builds the demo consumer and `vagrant up`s

`make test` to run the jepsen tests

if you need reprovisioning of vms then `vagrant provision`

alt+f4 does a super neat trick try that out

## vbox network requirements

`make ensureif`

when using virtualbox as a vagrant provider you need to be sure that you have a `host-only` network configured with the required ip range for the static IPs specified in the vagrantfile. I have one with the following settings:
- IP: `172.28.128.1`
- mask: `255.255.255.0`

use the `make ensureif` target (not run by default), or you can add one from *file > preferences > network > host-only* if you prefer clicky-clicking through vbox's ui
