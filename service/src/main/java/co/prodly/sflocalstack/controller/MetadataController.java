package co.prodly.sflocalstack.controller;

import co.prodly.sflocalstack.service.MetadataManifestParser;
import co.prodly.sflocalstack.service.MetadataService;
import co.prodly.sflocalstack.service.MetadataSoapParser;
import co.prodly.sflocalstack.service.MetadataSoapRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/services/Soap/m/60.0")
public class MetadataController {

    private final MetadataSoapParser parser;
    private final MetadataSoapRenderer renderer;
    private final MetadataService metadataService;
    private final MetadataManifestParser manifestParser;

    public MetadataController(MetadataSoapParser parser, MetadataSoapRenderer renderer,
                              MetadataService metadataService, MetadataManifestParser manifestParser) {
        this.parser = parser;
        this.renderer = renderer;
        this.metadataService = metadataService;
        this.manifestParser = manifestParser;
    }

    @PostMapping(consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> handle(@RequestBody String body) {
        try {
            MetadataSoapParser.ParsedSoapRequest request = parser.parse(body);
            String response = switch (request.operation()) {
                case "describeMetadata" -> renderer.renderDescribeMetadata(metadataService.describeMetadata());
                case "listMetadata" -> renderer.renderListMetadata(metadataService.listMetadata(allQueryTypes(request.values()), firstQueryFolder(request.values())));
                case "readMetadata" -> {
                    String type = String.valueOf(request.values().get("type"));
                    yield renderer.renderReadMetadata(type, metadataService.readMetadata(type, valuesAsList(request.values().get("fullNames"))));
                }
                case "deploy" -> renderer.renderDeploy(metadataService.deploy(String.valueOf(request.values().getOrDefault("ZipFile", ""))));
                case "checkDeployStatus" -> renderer.renderCheckDeployStatus(metadataService.checkDeployStatus(String.valueOf(request.values().get("asyncProcessId"))));
                case "cancelDeploy" -> renderer.renderCancelDeploy(metadataService.cancelDeploy(String.valueOf(request.values().get("asyncProcessId"))));
                case "retrieve" -> renderer.renderRetrieve(metadataService.retrieve(manifestParser.extractTypeRequests(request.values())));
                case "checkRetrieveStatus" -> renderer.renderCheckRetrieveStatus(metadataService.checkRetrieveStatus(String.valueOf(request.values().get("asyncProcessId"))));
                default -> renderer.renderFault("soapenv:Client", "Unsupported metadata operation: " + request.operation());
            };
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(response);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(renderer.renderFault("soapenv:Client", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(renderer.renderFault("soapenv:Client", ex.getMessage()));
        }
    }

    private List<String> allQueryTypes(Map<String, Object> values) {
        Object queries = values.get("queries");
        if (queries instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map<?, ?>)
                    .map(item -> String.valueOf(((Map<?, ?>) item).get("type")))
                    .filter(t -> t != null && !"null".equals(t))
                    .toList();
        }
        return List.of();
    }

    private String firstQueryType(Map<String, Object> values) {
        Object queries = values.get("queries");
        if (queries instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            return String.valueOf(first.get("type"));
        }
        return null;
    }

    private String firstQueryFolder(Map<String, Object> values) {
        Object queries = values.get("queries");
        if (queries instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            Object folder = first.get("folder");
            return folder == null ? null : String.valueOf(folder);
        }
        return null;
    }

    private List<String> valuesAsList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }
}
