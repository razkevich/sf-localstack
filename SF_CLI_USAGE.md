# SF CLI Usage

## Start the local emulator

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
cd /Users/razkevich/code/sf_localstack
sdk env install
mvn -pl service spring-boot:run
```

## Add a local org alias

```bash
export SF_ACCESS_TOKEN='00D000000000001!FAKE_ACCESS_TOKEN'

sf org login access-token \
  --instance-url http://localhost:8080 \
  --alias sf-local \
  --set-default \
  --no-prompt
```

## Inspect the alias

```bash
sf org list
sf org display --target-org sf-local
```

## Query data

```bash
sf data query \
  --target-org sf-local \
  --query "SELECT Id, Name FROM Account"
```

## Create a record

```bash
sf data create record \
  --target-org sf-local \
  --sobject Account \
  --values "Name='CLI Account' Industry='Testing'"
```

## Update a record

```bash
sf data update record \
  --target-org sf-local \
  --sobject Account \
  --where "Name='CLI Account'" \
  --values "Phone='555-9999'"
```

## Delete a record

```bash
sf data delete record \
  --target-org sf-local \
  --sobject Account \
  --where "Name='CLI Account'"
```

## Call raw REST endpoints

```bash
sf api request rest \
  --target-org sf-local \
  --method GET \
  /services/data/v60.0/sobjects/Account/describe
```

## Tooling/helper queries

```bash
sf api request rest \
  --target-org sf-local \
  --method GET \
  "/services/data/v60.0/tooling/query?q=SELECT%20Name%20FROM%20TabDefinition"

## Query FlowDefinitionView

```bash
sf api request rest \
  --target-org sf-local \
  --method GET \
  "/services/data/v60.0/query?q=SELECT%20DurableId%20FROM%20FlowDefinitionView%20WHERE%20ApiName%20%3D%20'LoginFlow'"
```

## Metadata SOAP via SF CLI

```bash
sf api request rest \
  --target-org sf-local \
  --method POST \
  --header 'Content-Type:text/xml' \
  --body '<?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:met="http://soap.sforce.com/2006/04/metadata"><soapenv:Body><met:describeMetadata/></soapenv:Body></soapenv:Envelope>' \
  /services/Soap/m/60.0
```
```

## Reset local state

```bash
sf api request rest \
  --target-org sf-local \
  --method POST \
  /reset
```

## Remove the alias

```bash
sf org logout --target-org sf-local --no-prompt
```

## Notes

- `sf` warns about `http://localhost`; that is expected for local use.
- The alias login works because the emulator returns a seeded local `User` record for the CLI auth lookup.
