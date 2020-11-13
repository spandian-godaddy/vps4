## How to run Phase 3 tests locally

1. Build your code with `mvn -B verify` or the IDE of your choosing
2. Change your working directory to `vps4-phase3-tests/target`
3. Run the following command. Change the URLs, image names, and credentials to match the environment and images you want to test. You might have to get the admin password from someone on the team.
   ```
   java -ea -jar vps4-phase3-tests-0.0.1-SNAPSHOT.jar --api-url https://vps4.api.stg-godaddy.com/ --shopper YOUR_SHOPPER_ID --password 'YOUR_IDP_PASSWORD' --sso-url https://sso.stg-godaddy.com/v1/api/token --admin 02d1700936szpA --admin-pass 'THE_ADMIN_PASSWORD' --max-vms 2 --pool-size 1 --vm-timeout 1800 --images 'centos7_64,hfs-ubuntu-1604'
   ```
5. See the [MacOS instructions](#On-MacOS) if you want to test Windows images from your laptop

## Testing Windows images

Testing Windows images requires the `winexe` program. It is used to run Powershell commands remotely. The phase 3 tests will still run if winexe is not installed, but they will be limited to testing Linux images.

#### On Linux

Winexe is not available in any mainstream package managers. Buidling it from source is also difficult since some dependencies it needs (specifically the `mingw*` packages) have been removed. It also uses a deprecated version of SMB by default which causes occassional errors when trying to connect. Instead, install [this patched binary](vps4-phase3-tests/winexe/winexe-static) which supports SMB2.
```
cp vps4-phase3-tests/winexe/winexe-static /usr/bin/winexe && chmod +x /usr/bin/winexe
```

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