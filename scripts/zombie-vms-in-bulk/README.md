## Zombie VMs in Bulk

This script was used on 1/25/24 as part of the openstack EOL. It iterates over a list of VMs and calls the VPS4 API to zombie them. We noticed several VMs weren't being zombied even though our API showed no errors. It turns out the `productId`s in their credits were `null`. For some, this was because the VMs failed to be provisioned and the customer never retried. For others, the cause was unknown.

If the script detects a `null` product ID and a failed CREATE_VM action, it just destroys the VM. If it detects a `null` product ID but no other issues, it patches the HFS product meta so that the product ID is no longer null, then zombies the VM.

## Certs

You'll need the HFS API certs. These can be found in the creds repo.
