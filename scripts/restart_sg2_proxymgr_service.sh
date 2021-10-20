#!/usr/bin/env bash
ssh 02586d6e7eecm@sg2plvps4wp02.cloud.sin2.gdg `sudo systemctl restart proxymgr`;
ssh 02586d6e7eecm@sg2plvps4wp01.cloud.sin2.gdg `sudo systemctl restart proxymgr`;