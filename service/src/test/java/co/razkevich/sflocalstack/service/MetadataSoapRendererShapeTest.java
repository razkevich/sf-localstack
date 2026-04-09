package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.metadata.model.MetadataCatalogEntry;
import co.razkevich.sflocalstack.metadata.service.MetadataService;
import co.razkevich.sflocalstack.metadata.service.MetadataSoapRenderer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataSoapRendererShapeTest {

    private final MetadataSoapRenderer renderer = new MetadataSoapRenderer();

    @Test
    void describeMetadataIncludesSuffix() {
        MetadataCatalogEntry entry = new MetadataCatalogEntry(
                "ApexClass", "MyClass", "classes/MyClass.cls", "classes", false, true, Instant.now(), "cls");

        String xml = renderer.renderDescribeMetadata(List.of(entry));

        assertThat(xml).contains("<suffix>cls</suffix>");
        assertThat(xml).contains("<xmlName>ApexClass</xmlName>");
    }

    @Test
    void describeMetadataOmitsSuffixElementWhenNull() {
        MetadataCatalogEntry entry = new MetadataCatalogEntry(
                "RecordType", "Master", "objects/Account.object", "objects", false, true, Instant.now(), null);

        String xml = renderer.renderDescribeMetadata(List.of(entry));

        assertThat(xml).contains("<xmlName>RecordType</xmlName>");
        assertThat(xml).doesNotContain("<suffix>null</suffix>");
    }

    @Test
    void readMetadataStandardValueSetIncludesSorted() {
        MetadataService.ReadMetadataRecord record = new MetadataService.ReadMetadataRecord(
                "StandardValueSet", "valueSet", "valueSet", Map.of());

        String xml = renderer.renderReadMetadata("StandardValueSet", List.of(record));

        assertThat(xml).contains("<sorted>false</sorted>");
        assertThat(xml).contains("xsi:type=\"StandardValueSet\"");
        assertThat(xml).contains("<fullName>valueSet</fullName>");
    }

    @Test
    void readMetadataStandardValueSetSortedAppearsBeforeStandardValue() {
        MetadataService.ReadMetadataRecord record = new MetadataService.ReadMetadataRecord(
                "StandardValueSet", "valueSet", "valueSet", Map.of());

        String xml = renderer.renderReadMetadata("StandardValueSet", List.of(record));

        int sortedPos = xml.indexOf("<sorted>");
        int standardValuePos = xml.indexOf("<standardValue>");
        assertThat(sortedPos).isLessThan(standardValuePos);
    }
}
