#!/usr/bin/env bash
ssh 02586d6e7eecm@n3plvps4wp01.cloud.ams3.gdg `sudo systemctl restart proxymgr`;
ssh 02586d6e7eecm@n3plvps4wp02.cloud.ams3.gdg `sudo systemctl restart proxymgr`;