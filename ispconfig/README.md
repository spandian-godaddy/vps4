ISPConfig Info
=======

# Scripts
ISPConfig image contains custom scripts imbedded into the image

## Reset MySQL root password
- [ispconfig-init.service](ispconfig-init.service) -> /lib/systemd/system/ispconfig-init.service  
- [sql-password-reset](sql-password-reset) -> /usr/local/bin/sql-password-reset  

It is important that each VM built from the ISPConfig image contain a unique
MySQL root password.  Systemd will start the `ispconfig-init` service which will
run the `sql-reset-password` script if it exists.  That script will set the root
MySQL password to a unique 16 character password for each VM.  The script will
then remove itself so systemd will not run the service on future boots.

