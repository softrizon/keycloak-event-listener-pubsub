# keycloak-event-listener-google-cloud-pubsub

Configuration

Environment variables
GOOGLE_APPLICATION_CREDENTIALS=/path/to/google/service_account_key_file
SN_KC_PUBSUB_TOPIC_ID=topic_id
SN_KC_PUBSUB_USER_EVENT_TYPES=USER:*:*:*:*,USER:*:*:*:REGISTER,USER:*:*:*:UPDATE_EMAIL
SN_KC_PUBSUB_ADMIN_EVENT_TYPES=ADMIN:*:*:*:*


Event representation published to pub/sub 

## Message attributes
format = JSON_API_V1
who=ADMIN | USER
realmId = master
result= SUCCESS | ERROR
clientId=a2a3d88f-e42e-4ce2-b134-707cd745e352
resourceType=USER
eventType=REGISTER
operationType=UPDATE
pattern =
    USER:<REALM_ID>:<RESULT = SUCCESS | ERROR>:<CLIENT_ID>:<EVENT_TYPE>
    ADMIN:<REALM_ID>:<RESULT = SUCCESS | ERROR>:<RESOURCE_TYPE>:<OPERATION_TYPE>

to accept any value on specific field, put a wildcard * on it

USER.*.*.*.*
USER.ORCAS-CUPID.SUCCESS.a2a3d88f-e42e-4ce2-b134-707cd745e352.IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR
USER.ORCAS-CUPID.SUCCESS.*.IDENTITY_PROVIDER_RETRIEVE_TOKEN_ERROR

ADMIN.*.*.*.*
ADMIN.ORCAS-CUPID.SUCCESS.AUTHORIZATION_RESOURCE_SERVER.CREATE
