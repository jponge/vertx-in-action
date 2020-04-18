#!/bin/sh
echo 'NOTE: the token in this script may need to be updated'
hey -z $2 \
    -o csv \
    -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJkZXZpY2VJZCI6InAtMTIzNDU2LWFiY2RlZiIsImlhdCI6MTU4NzE1NTM3NCwiZXhwIjoxNTg3NzYwMTc0LCJpc3MiOiIxMGstc3RlcHMtYXBpIiwic3ViIjoibG9hZHRlc3RpbmctdXNlciJ9.ogRJYPlhsZvuiF9VujTfxcvgocEcu1Jt1FAvF1BFT9oR0S7IakLwQXhHC0QvYj3IvcPdSTcO0hwWWUgqFZtRMBeZmq2iH5gBx7L4ca26lSuc1jkwRTnNYlqFMm-mj1kC_zm19fSMCyh6BYcv7bGJuvqoI_2aFkaCRFGeIW8YBj2ykZGq5BszgHh1V0gatJL2N9bU_fwShFcbHkcoKIqGEwKZv7mG3LN_0gzz3f8pOczK_04fvto_hkHPCodlcDmMVKzuNtQswode1wIzbH_a7PtqKRDVevFAjsBnCpAnWVpHxWxUTKyKE864_XsC6utEWfNyWlCI83NjBKWcghyDpg' \
    http://$1:4000/api/v1/loadtesting-user/total \
    > data/hey-run-steps-z$2.csv
