# service-conll2tcf

WebLicht web-service that converts a conllu file (produced by the conll-services in weblicht) into a TCF file.

### How to build and test

Build with `mvn clean package`.
Run the web server with `./target/appassembler/bin/service-conll2tcf server service-config.yaml`

Test with:

```
curl -v -H 'content-type:application/conllu' --data-binary @diefee.conllu 'localhost:8080/service-conll2tcf/annotate/convert/stream/?language=en'
```
