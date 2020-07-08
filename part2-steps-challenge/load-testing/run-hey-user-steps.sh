#!/bin/bash
echo 'NOTE: the token in this script may need to be updated'
hey -z $2 \
    -o csv \
    -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJkZXZpY2VJZCI6InAtMTIzNDU2LWFiY2RlZiIsImlhdCI6MTU4NzQ5MDk0OSwiZXhwIjoxNTg4MDk1NzQ5LCJpc3MiOiIxMGstc3RlcHMtYXBpIiwic3ViIjoibG9hZHRlc3RpbmctdXNlciJ9.jA31mU0ZgrI2FHHBVseo-d5lhCkfO4ULUq8yiRvJtaOB2gAgyXdA6RiZsqp6ex9mrRp82Ci3rn_yLX0cICLHq-ZemhoyeIzSV_4WUfLRQ-muIDF9UheYobOMieYLje5oGe7pi7ny8DEVUJ5kC4No5-KKviIpHmsaYPP528hjbjWUfP0LfQK2b1FMc1mnGVVJa4R6RzEW7oCGlsjuUUbUzffi11XZXfchecYvwwWqFyEwlDcr3ZI95qS5pZgnt-7-tkykD0Y0RFXir3bnQR4wUAxFF4PLG33IyHgf3ZmXFKx1I7fa4GnYrqPGfRGiL8-Qm0VPJJMzD8EhejbgkmM2Gw' \
    http://$1:4000/api/v1/loadtesting-user/total \
    > data/hey-run-steps-z$2.csv
