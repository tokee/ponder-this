/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.ekot.misc;


import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sample code for using {@link XMLStreamWriter} for producing SolrXMLDocuments.
 */
public class XMLStreamExperiment {
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
    
    public static void main(String[] args) throws IOException, XMLStreamException {
        // Use a FileOutputStream instead of a ByteArrayOutputStream to write directly to file
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(bout, "utf-8");
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeStartElement("add");
            xml.writeCharacters("\n");
            for (int i = 0 ; i < 3 ; i++) {
                xml.writeCharacters("  ");
                xml.writeStartElement("doc");
                xml.writeCharacters("\n");
                for (int j = 0 ; j < 4 ; j++) {
                    xml.writeCharacters("    ");
                    xml.writeStartElement("field");
                    xml.writeAttribute("name", "field" + j);
                    xml.writeCharacters("Field content #" + i + ", " + j);
                    xml.writeEndElement(); // field
                    xml.writeCharacters("\n");
                }
                xml.writeCharacters("  ");
                xml.writeEndElement(); // doc
                xml.writeCharacters("\n");
            }
            xml.writeEndElement(); // add
            xml.writeCharacters("\n");
            xml.writeEndDocument();
            xml.flush();

            System.out.println(bout.toString("utf-8"));
        }
    }
    /* Output:
<?xml version="1.0" encoding="UTF-8"?>
<add>
  <doc>
    <field name="field0">Field content #0, 0</field>
    <field name="field1">Field content #0, 1</field>
    <field name="field2">Field content #0, 2</field>
    <field name="field3">Field content #0, 3</field>
  </doc>
  <doc>
    <field name="field0">Field content #1, 0</field>
    <field name="field1">Field content #1, 1</field>
    <field name="field2">Field content #1, 2</field>
    <field name="field3">Field content #1, 3</field>
  </doc>
  <doc>
    <field name="field0">Field content #2, 0</field>
    <field name="field1">Field content #2, 1</field>
    <field name="field2">Field content #2, 2</field>
    <field name="field3">Field content #2, 3</field>
  </doc>
</add>
     */
}
