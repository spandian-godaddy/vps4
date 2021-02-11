## When To Run Phase3 Tests

1. When code gets merged to "vps4" git repo **master** branch. As part of CICD pipeline it runs [test-stage](https://vps4.jenkins.int.godaddy.com/job/test-stage/) Jenkins job which triggers [stage_phase_3_test](https://vps4.jenkins.int.godaddy.com/view/all/job/stage_phase_3_test/) build job who runs basic phase3 tests on *vps4-centos-7-cpanel-11,vps4-windows-2016-plesk-18* OpenStack images in VPS4 **Stage environment**.
2. When deploying "vps4" code to Production. The [prod deployment job](https://vps4.jenkins.int.godaddy.com/job/prod/) triggers build jobs including [prod_n3_phase_3_test](https://vps4.jenkins.int.godaddy.com/view/all/job/prod_n3_phase_3_test/), [prod_sg2_phase_3_test](https://vps4.jenkins.int.godaddy.com/view/all/job/prod_sg2_phase_3_test/) and [prod_a2_phase_3_test](https://vps4.jenkins.int.godaddy.com/view/all/job/prod_a2_phase_3_test/).
These build jobs run basic phase3 tests on *vps4-centos-7-cpanel-11* OpenStack image in VPS4 **N3, SG2, A2 environments** respectively.
3. Full suite of phase3 tests run daily by [daily_full_stage](https://vps4.jenkins.int.godaddy.com/job/daily_full_stage/) Jenkins job. This job runs around 2am MST daily (load balanced in Jenkins). And it runs all existing phase3 tests against a wide range of images (as of Feb 11 2021: all OpenStack images, all OptimizedHosting images, and a random OVH image are tested) in VPS4 **Stage environment**.

* Note that (1) and (2) only run basic "smoke test" (passed by the "--smoke-test true" option), which is the *ChangeHostnameTest*. However, (3) runs the full suite of tests which are part of the [critical use cases](https://confluence.godaddy.com/display/HOSTING/VPS4+Critical+Use+Cases).
* Also note that (1) and (3) are using the shopper ID **196569383**, but (2) uses a different shopper ID **227572632**.  Besides, (3) uses a service account **SVCM2AbYa3jVxBHWa** which has DEV-VPS4 privilege to run tests that require sysAdmin permission.

## Phase3 Tests Overview

Tests run on different VMs of different image types in parallel.
* The "--pool-size" option specifies the number of VMs that can be created **PER IMAGE**.
* The "--max-vms" option specifies the **TOTAL** number of VMs that can be created, if there are enough credits.
* Each test kicks off a new test thread to run on each image (test group).
* For example, with the following command 8 different image are being tested. For each of the image type, 2 VMs are created. In total 16 Vms can be created. And for each of the 16 VMs, only a subset of tests run on it. However, each type of image is guaranteed to have the full suite of tests covered.
  ```
  java -ea -jar /opt/vps4/phase3_tests/vps4-phase3-tests-0.0.1-2188.jar --api-url https://vps4.api.stg-godaddy.com/ --shopper 196569383 --password [HIDDEN] --sso-url https://sso.godaddy.com/v1/api/token --admin SVCM2AbYa3jVxBHWa --admin-pass [HIDDEN] --max-vms 16 --pool-size 2 --vm-timeout 5400 --images hfs-centos-7,vps4-centos-7-plesk-18,vps4-centos-7-cpanel-11,hfs-windows-2016,vps4-windows-2016-plesk-18,hfs-ubuntu-1604,win2019-std_64,hfs-ubuntu2004
  ```
* It tries to deletes all existing VMs owned the specified shopper (passed by the "--shopper" option) after the tests finish, and before starting the tests.

## How To Run Phase3 Tests LOCALLY

1. Build your code with `mvn -B verify` or the IDE of your choosing
2. Change your working directory to `vps4-phase3-tests/target`
3. Run the following command. Change the URLs, image names, and credentials to match the environment and images you want to test. You might have to get the admin password from someone on the team.
   ```
   java -ea -jar vps4-phase3-tests-0.0.1-SNAPSHOT.jar --api-url https://vps4.api.stg-godaddy.com/ --shopper YOUR_SHOPPER_ID --password '[YOUR_SHOPPER_PWD]' --sso-url https://sso.godaddy.com/v1/api/token --admin SVCM2AbYa3jVxBHWa --admin-pass '[FIND_PWD_IN_CYBERARK_OR_ASK_TEAM]' --max-vms 2 --pool-size 1 --vm-timeout 1800 --images 'vps4-centos-7-cpanel-11'
   ```
4. See the [Winexe on MacOS instructions](#On-MacOS) if you want to test Windows images from your laptop.

## Testing Windows images

Testing Windows images requires the `winexe` program. It is used to run Powershell commands remotely. The phase 3 tests will still run if winexe is not installed, but they will be limited to testing Linux images.

#### On Linux

Winexe is not available in any mainstream package managers. Buidling it from source is also difficult since some dependencies it needs (specifically the `mingw*` packages) have been removed. It also uses a deprecated version of SMB by default which causes occassional errors when trying to connect. Instead, install [this patched binary](winexe/winexe-static) which supports SMB2.
```
sudo mkdir /usr/bin/winexe
sudo cp vps4-phase3-tests/winexe/winexe-static /usr/bin/winexe/winexe
sudo chown root:root winexe
sudo chmod 755 /usr/bin/winexe/winexe
```
Winexe is installed in `/usr/bin/winexe` and not `/usr/bin` because the `/usr/bin/winexe` directory is excluded from Trend Micro. Trend Micro will automatically delete it if you store it elsewhere. Make sure `/usr/bin/winexe` is included in the path of whichever user plans to run it.

#### On MacOS

Brew offers a winexe package but it is broken and no longer maintained. Either way, we need the patched version which only runs on Linux. We can use docker to get around this.
1. Install docker on your machine
2. Build the winexe image
   ```
   cd vps4-phase3-tests/winexe
   docker build --tag winexe:1.1 .
   ```

Now you can use winexe like this:
```
docker run -it --rm winexe:1.1 winexe --reinstall -U 'username'%'password' //127.0.0.1 'netstat -a'
```
