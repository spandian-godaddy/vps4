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

When adding these scripts to the ispconfig hfs image, make sure to enable the ispconfig-init service in systemd:
```bash
systemctl enable ispconfig-init
```
Also, ensure the `sql-password-reset` script has execute permissions:
```bash
chmod 700 /usr/local/bin/sql-password-reset
```

# Creating a new ISPConfig Image
### Prepare the VM:
1. Create vm from existing ipsconfig image
1. Enable admin (sudo permissions)
1. Create systemd file and post boot bash script
1. Set exec permissions on bash script
1. Enable systemd ispconfig-init service
1. Clean VM manually (See the next section)
1. Stop VM
1. Create snapshot without clean param
1. Call hfs imageCreated in hfs vms/ api
1. Call hfs imageTested
1. Publish snapshot
1. Call hfs vms imagePublished
1. Call hfs vms imagePublic

### Clean the VM:
1. grep -qc set_hostname /etc/cloud/cloud.cfg || sudo sed -i 's/resizefs/&\n - set_hostname\n - update_hostname/' /etc/cloud/cloud.cfg
1. sed -i 's/^preserve_hostname:.*$/preserve_hostname: false/' /etc/cloud/cloud.cfg
1. rm -f /etc/ssh/ssh_host_*
1. rm -f /opt/thespian/director/thespian_system.log*
1. sed -i '2,$d' ~/.mysql_history
1. journalctl --rotate
1. journalctl --vacuum-time=1s
1. history -c
1. Get console and login as root and delete user
```userdel -fr <username>```


# Changes made to the image
1. Go to the server config page in ISPConfig and change the server name to "server"
1. On the IP Address page in server config remove all IP Addresses (The customer will need to add in their correct IP address)
1. SSH to the box and edit /var/www/roundcube/config/config.inc.php - find the line with ```$config['smtp_user'] = '%u';``` and update it to like this:
```$config['smtp_user'] = '';```
1. While still connected to the box, edit the file /etc/apache2/sites-available/roundcube.conf, erase the contents and replace with this block:

```
<VirtualHost *:80>
  # Virtual host configuration + information (replicate changes to *:443 below)
 # ServerAdmin webmaster@example.com
 # ServerName 148.66.133.161
  DocumentRoot /var/www/roundcube
  ErrorLog /var/log/apache2/roundcube.error.log
  CustomLog /var/log/apache2/roundcube.access.log combined

  # Permanently redirect all HTTP requests to HTTPS
  RewriteEngine on
  RewriteCond %{SERVER_PORT} !^443$
  RewriteRule ^/(.*) https://%{HTTP_HOST}/$1 [NC,R=301,L]

  <Directory /var/www/roundcube>
      Options +FollowSymLinks
      DirectoryIndex index.php

      <IfModule mod_php5.c>
        AddType application/x-httpd-php .php

        php_flag magic_quotes_gpc Off
        php_flag track_vars On
        php_flag register_globals Off
        php_value include_path .:/usr/share/php
      </IfModule>
      Options -Indexes
      AllowOverride All
      Order allow,deny
      allow from all
  </Directory>
</VirtualHost>

<IfModule mod_ssl.c>
<VirtualHost *:443>
 # Virtual host configuration + information (replicate changes to *:80 above)
 #ServerAdmin webmaster@example.com
 #ServerName webmail.example.com
  DocumentRoot /var/www/roundcube
  ErrorLog /var/log/apache2/roundcube.error.log
  CustomLog /var/log/apache2/roundcube.access.log combined

 # SSL certificate + engine configuration
  SSLEngine on
  SSLCertificateFile /usr/local/ispconfig/interface/ssl/ispserver.crt
  SSLCertificateKeyFile /usr/local/ispconfig/interface/ssl/ispserver.key

 # Roundcube directory permissions + restrictions
  <Directory /var/www/roundcube>
    Options -Indexes
    AllowOverride All
  </Directory>
  <Directory /var/www/roundcube/config>
    Order Deny,Allow
    Deny from All
  </Directory>
  <Directory /var/www/roundcube/temp>
    Order Deny,Allow
    Deny from All
  </Directory>
  <Directory /var/www/roundcube/logs>
    Order Deny,Allow
    Deny from All
  </Directory>
</VirtualHost>
</IfModule>
```  
After completing these steps restart Apache: ```sudo systemctl restart apache2.service```
