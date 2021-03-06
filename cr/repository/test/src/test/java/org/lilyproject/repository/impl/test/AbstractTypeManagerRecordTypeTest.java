/*
 * Copyright 2010 Outerthought bvba
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lilyproject.repository.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.lilyproject.repository.api.Scope.VERSIONED;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.lilyproject.repository.api.FieldType;
import org.lilyproject.repository.api.FieldTypeEntry;
import org.lilyproject.repository.api.FieldTypeNotFoundException;
import org.lilyproject.repository.api.QName;
import org.lilyproject.repository.api.RecordType;
import org.lilyproject.repository.api.RecordTypeBuilder;
import org.lilyproject.repository.api.RecordTypeExistsException;
import org.lilyproject.repository.api.RecordTypeNotFoundException;
import org.lilyproject.repository.api.RepositoryException;
import org.lilyproject.repository.api.SchemaId;
import org.lilyproject.repository.api.Scope;
import org.lilyproject.repository.api.TypeException;
import org.lilyproject.repository.api.TypeManager;
import org.lilyproject.repository.impl.id.SchemaIdImpl;

public abstract class AbstractTypeManagerRecordTypeTest {

    private static String namespace1 = "ns1";
    protected static TypeManager typeManager;
    protected static FieldType fieldType1;
    protected static FieldType fieldType2;
    protected static FieldType fieldType3;

    protected static void setupFieldTypes() throws Exception {
        fieldType1 = typeManager.createFieldType(typeManager.newFieldType(typeManager.getValueType("STRING"),
                new QName(namespace1, "field1"), Scope.NON_VERSIONED));
        fieldType2 = typeManager.createFieldType(typeManager.newFieldType(typeManager.getValueType("INTEGER"),
                new QName(namespace1, "field2"), Scope.VERSIONED));
        fieldType3 = typeManager.createFieldType(typeManager.newFieldType(typeManager.getValueType("BOOLEAN"),
                new QName(namespace1, "field3"), Scope.VERSIONED_MUTABLE));
    }

    @Test
    public void testCreateEmpty() throws Exception {
        QName name = new QName("testNS", "testCreateEmpty");
        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);
        assertEquals(Long.valueOf(1), recordType.getVersion());
        RecordType recordType2 = typeManager.getRecordTypeByName(name, null);
        assertEquals(recordType, recordType2);
    }

    @Test
    public void testCreate() throws Exception {
        QName name = new QName("testNS", "testCreate");
        RecordType recordType = typeManager.newRecordType(name);
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        RecordType createdRecordType = typeManager.createRecordType(recordType);
        assertEquals(Long.valueOf(1), createdRecordType.getVersion());

        recordType.setVersion(Long.valueOf(1));
        recordType.setId(createdRecordType.getId());
        assertEquals(recordType, typeManager.getRecordTypeById(createdRecordType.getId(), null));
    }

    @Test
    public void testCreateSameNameFails() throws Exception {
        QName name = new QName(namespace1, "testCreateSameNameFails");
        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);

        recordType = typeManager.newRecordType(name);
        try {
            System.out.println("Expecting RecordTypeExistsException");
            typeManager.createRecordType(recordType);
            fail();
        } catch (RecordTypeExistsException expected) {
        }
    }

    @Test
    public void testUpdate() throws Exception {
        QName name = new QName(namespace1, "testUpdate");
        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);
        assertEquals(Long.valueOf(1), recordType.getVersion());
        RecordType recordTypeV1 = typeManager.getRecordTypeByName(name, null);
        assertEquals(Long.valueOf(1), typeManager.updateRecordType(recordTypeV1).getVersion());

        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), true));
        RecordType recordTypeV2 = typeManager.updateRecordType(recordType);
        assertEquals(Long.valueOf(2), recordTypeV2.getVersion());
        assertEquals(Long.valueOf(2), typeManager.updateRecordType(recordTypeV2).getVersion());

        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), true));
        RecordType recordTypeV3 = typeManager.updateRecordType(recordType);
        assertEquals(Long.valueOf(3), recordTypeV3.getVersion());
        assertEquals(Long.valueOf(3), typeManager.updateRecordType(recordType).getVersion());

        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), true));
        RecordType recordTypeV4 = typeManager.updateRecordType(recordType);
        assertEquals(Long.valueOf(4), recordTypeV4.getVersion());
        assertEquals(Long.valueOf(4), typeManager.updateRecordType(recordType).getVersion());

        recordType.setVersion(Long.valueOf(4));
        assertEquals(recordType, typeManager.getRecordTypeByName(name, null));

        // Read old versions
        assertEquals(recordTypeV1, typeManager.getRecordTypeByName(name,Long.valueOf(1)));
        assertEquals(recordTypeV2, typeManager.getRecordTypeByName(name,Long.valueOf(2)));
        assertEquals(recordTypeV3, typeManager.getRecordTypeByName(name,Long.valueOf(3)));
        assertEquals(recordTypeV4, typeManager.getRecordTypeByName(name,Long.valueOf(4)));
    }

    @Test
    public void testReadNonExistingRecordTypeFails() throws Exception {
        QName name = new QName("testNS", "testReadNonExistingRecordTypeFails");
        try {
            System.out.println("Expecting RecordTypeNotFoundException");
            typeManager.getRecordTypeByName(name, null);
            fail();
        } catch (RecordTypeNotFoundException expected) {
        }

        typeManager.createRecordType(typeManager.newRecordType(name));
        try {
            System.out.println("Expecting RecordTypeNotFoundException");
            typeManager.getRecordTypeByName(name, Long.valueOf(2));
            fail();
        } catch (RecordTypeNotFoundException expected) {
        }
    }

    @Test
    public void testUpdateNonExistingRecordTypeFails() throws Exception {
        QName name = new QName("testNS", "testUpdateNonExistingRecordTypeFails");
        RecordType recordType = typeManager.newRecordType(name);
        try {
            System.out.println("Expecting RecordTypeNotFoundException");
            typeManager.updateRecordType(recordType);
            fail();
        } catch (RecordTypeNotFoundException expected) {
        }
        recordType.setId(new SchemaIdImpl(UUID.randomUUID()));
        try {
            System.out.println("Expecting RecordTypeNotFoundException");
            typeManager.updateRecordType(recordType);
            fail();
        } catch (RecordTypeNotFoundException expected) {
        }
    }

    @Test
    public void testFieldTypeExistsOnCreate() throws Exception {
        QName name = new QName("testNS", "testUpdateNonExistingRecordTypeFails");
        RecordType recordType = typeManager.newRecordType(name);
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(new SchemaIdImpl(UUID.randomUUID()), false));
        try {
            System.out.println("Expecting FieldTypeNotFoundException");
            typeManager.createRecordType(recordType);
            fail();
        } catch (FieldTypeNotFoundException expected) {
        }
    }

    @Test
    public void testFieldTypeExistsOnUpdate() throws Exception {
        QName name = new QName("testNS", "testFieldGroupExistsOnUpdate");
        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);

        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(new SchemaIdImpl(UUID.randomUUID()), false));
        try {
            System.out.println("Expecting FieldTypeNotFoundException");
            typeManager.updateRecordType(recordType);
            fail();
        } catch (FieldTypeNotFoundException expected) {
        }
    }

    @Test
    public void testRemove() throws Exception {
        QName name = new QName("testNS", "testRemove");
        RecordType recordType = typeManager.newRecordType(name);
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        recordType = typeManager.createRecordType(recordType);

        recordType.removeFieldTypeEntry(fieldType1.getId());
        recordType.removeFieldTypeEntry(fieldType2.getId());
        recordType.removeFieldTypeEntry(fieldType3.getId());
        typeManager.updateRecordType(recordType);

        RecordType readRecordType = typeManager.getRecordTypeByName(name, null);
        assertTrue(readRecordType.getFieldTypeEntries().isEmpty());
    }

    @Test
    public void testRemoveLeavesOlderVersionsUntouched() throws Exception {
        QName name = new QName("testNS", "testRemoveLeavesOlderVersionsUntouched");
        RecordType recordType = typeManager.newRecordType(name);
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        recordType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        recordType = typeManager.createRecordType(recordType);

        recordType.removeFieldTypeEntry(fieldType1.getId());
        recordType.removeFieldTypeEntry(fieldType2.getId());
        recordType.removeFieldTypeEntry(fieldType3.getId());
        typeManager.updateRecordType(recordType);

        RecordType readRecordType = typeManager.getRecordTypeByName(name, Long.valueOf(1));
        assertEquals(3, readRecordType.getFieldTypeEntries().size());
    }

    @Test
    public void testSupertype() throws Exception {
        QName supertypeName = new QName("supertypeNS", "testSupertype");
        RecordType supertypeRt = typeManager.newRecordType(supertypeName);
        supertypeRt.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeRt.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeRt.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeRt = typeManager.createRecordType(supertypeRt);

        QName recordName = new QName("recordNS", "testSupertype");
        RecordType recordType = typeManager.newRecordType(recordName);
        recordType.addSupertype(supertypeRt.getId(), supertypeRt.getVersion());
        recordType = typeManager.createRecordType(recordType);
        assertEquals(Long.valueOf(1), recordType.getVersion());
        assertEquals(recordType, typeManager.getRecordTypeById(recordType.getId(), null));
    }

    @Test
    public void testSupertypeLatestVersion() throws Exception {
        QName supertypeName = new QName("supertypeNS", "testSupertypeLatestVersion");
        RecordType supertypeType = typeManager.newRecordType(supertypeName);
        supertypeType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeType = typeManager.createRecordType(supertypeType);

        supertypeType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeType.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeType = typeManager.updateRecordType(supertypeType);

        QName recordName = new QName("recordNS", "testSupertypeLatestVersion");
        RecordType recordType = typeManager.newRecordType(recordName);
        recordType.addSupertype(supertypeType.getId());
        recordType = typeManager.createRecordType(recordType);
        assertEquals(Long.valueOf(1), recordType.getVersion());

        recordType.addSupertype(supertypeType.getId(), 2L); // Assert latest version of the supertype RecordType got filled in
        assertEquals(recordType, typeManager.getRecordTypeById(recordType.getId(), null));
    }

    @Test
    public void testSupertypeUpdate() throws Exception {
        QName supertypeName = new QName("supertypeNS", "testSupertypeUpdate");
        RecordType supertypeRt1 = typeManager.newRecordType(supertypeName);
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeRt1 = typeManager.createRecordType(supertypeRt1);

        QName supertypeName2 = new QName("supertypeNS", "testSupertypeUpdate2");
        RecordType supertypeRt2 = typeManager.newRecordType(supertypeName2);
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeRt2 = typeManager.createRecordType(supertypeRt2);

        QName recordName = new QName("recordNS", "testSupertypeUpdate");
        RecordType recordType = typeManager.newRecordType(recordName);
        recordType.addSupertype(supertypeRt1.getId(), supertypeRt1.getVersion());
        recordType = typeManager.createRecordType(recordType);

        recordType.addSupertype(supertypeRt2.getId(), supertypeRt2.getVersion());
        recordType = typeManager.updateRecordType(recordType);
        assertEquals(Long.valueOf(2), recordType.getVersion());
        assertEquals(recordType, typeManager.getRecordTypeById(recordType.getId(), null));
    }

    @Test
    public void testSupertypeRemove() throws Exception {
        QName supertypeName = new QName("supertypeNS", "testSupertypeRemove");
        RecordType supertypeRt1 = typeManager.newRecordType(supertypeName);
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeRt1.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeRt1 = typeManager.createRecordType(supertypeRt1);

        QName supertypeName2 = new QName("supertypeNS", "testSupertypeRemove2");
        RecordType supertypeRt2 = typeManager.newRecordType(supertypeName2);
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType1.getId(), false));
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType2.getId(), false));
        supertypeRt2.addFieldTypeEntry(typeManager.newFieldTypeEntry(fieldType3.getId(), false));
        supertypeRt2 = typeManager.createRecordType(supertypeRt2);

        QName recordTypeName = new QName("recordNS", "testSupertypeRemove");
        RecordType recordType = typeManager.newRecordType(recordTypeName);
        recordType.addSupertype(supertypeRt1.getId(), supertypeRt1.getVersion());
        recordType = typeManager.createRecordType(recordType);

        recordType.addSupertype(supertypeRt2.getId(), supertypeRt2.getVersion());
        recordType.removeSupertype(supertypeRt1.getId());
        recordType = typeManager.updateRecordType(recordType);
        assertEquals(Long.valueOf(2), recordType.getVersion());
        RecordType readRecordType = typeManager.getRecordTypeById(recordType.getId(), null);
        Map<SchemaId, Long> supertypes = readRecordType.getSupertypes();
        assertEquals(1, supertypes.size());
        assertEquals(Long.valueOf(1), supertypes.get(supertypeRt2.getId()));
    }

    @Test
    public void testGetRecordTypes() throws Exception {
        RecordType recordType = typeManager.createRecordType(typeManager.newRecordType(new QName("NS", "getRecordTypes")));
        Collection<RecordType> recordTypes = typeManager.getRecordTypes();
        assertTrue(recordTypes.contains(recordType));
    }

    @Test
    public void testGetFieldTypes() throws Exception {
        FieldType fieldType = typeManager.createFieldType(typeManager.newFieldType(typeManager.getValueType("STRING"), new QName("NS", "getFieldTypes"), Scope.NON_VERSIONED));
        Collection<FieldType> fieldTypes = typeManager.getFieldTypes();
        assertTrue(fieldTypes.contains(fieldType));
    }

    @Test
    public void testUpdateName() throws Exception {
        QName name = new QName(namespace1, "testUpdateName");
        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);
        assertEquals(name, recordType.getName());

        QName name2 = new QName(namespace1, "testUpdateName2");
        recordType.setName(name2);
        recordType = typeManager.updateRecordType(recordType);
        recordType = typeManager.getRecordTypeById(recordType.getId(), null);
        assertEquals(name2, recordType.getName());
    }

    @Test
    public void testUpdateNameToExistingNameFails() throws Exception {
        QName name = new QName(namespace1, "testUpdateNameToExistingNameFails");
        QName name2 = new QName(namespace1, "testUpdateNameToExistingNameFails2");

        RecordType recordType = typeManager.newRecordType(name);
        recordType = typeManager.createRecordType(recordType);
        assertEquals(name, recordType.getName());

        RecordType recordType2 = typeManager.newRecordType(name2);
        recordType2 = typeManager.createRecordType(recordType2);

        recordType.setName(name2);
        try {
            System.out.println("Expecting TypeException");
            recordType = typeManager.updateRecordType(recordType);
            fail();
        } catch (TypeException expected){
        }
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        String NS = "testCreateOrUpdateRecordType";

        FieldType field1 = typeManager.createFieldType("STRING", new QName(NS, "field1"), Scope.NON_VERSIONED);
        FieldType field2 = typeManager.createFieldType("STRING", new QName(NS, "field2"), Scope.NON_VERSIONED);
        FieldType field3 = typeManager.createFieldType("STRING", new QName(NS, "field3"), Scope.NON_VERSIONED);

        RecordType recordType = typeManager.newRecordType(new QName(NS, "type1"));
        recordType.addFieldTypeEntry(field1.getId(), false);
        recordType.addFieldTypeEntry(field2.getId(), false);

        recordType = typeManager.createOrUpdateRecordType(recordType);
        assertNotNull(recordType.getId());

        // Without changing anything, do an update
        RecordType updatedRecordType = typeManager.createOrUpdateRecordType(recordType);
        assertEquals(recordType, updatedRecordType);

        // Remove the id from the record type and do a change
        recordType.setId(null);
        recordType.addFieldTypeEntry(field3.getId(), false);
        typeManager.createOrUpdateRecordType(recordType);
        recordType = typeManager.getRecordTypeByName(new QName(NS, "type1"), null);
        assertEquals(3, recordType.getFieldTypeEntries().size());
    }

    @Test
    public void testRecordTypeBuilderBasics() throws Exception {
        RecordTypeBuilder builder = typeManager.recordTypeBuilder();
        try {
            builder.create();
            fail("Exception expected since name of recordType is not specified");
        } catch (Exception expected) {
        }
        QName rtName = new QName("builderNS", "builderName");
        builder.name(rtName);
        builder.field(fieldType1.getId(), false);
        builder.field(fieldType2.getId(), true);
        RecordType recordType = builder.create();

        RecordType readRecordType = typeManager.getRecordTypeByName(rtName, null);
        assertEquals(recordType, readRecordType);
        assertFalse(readRecordType.getFieldTypeEntry(fieldType1.getId()).isMandatory());
        assertTrue(readRecordType.getFieldTypeEntry(fieldType2.getId()).isMandatory());

        builder.reset();
        builder.id(recordType.getId());
        recordType = builder.update();
        readRecordType = typeManager.getRecordTypeByName(rtName, null);
        assertEquals(recordType, readRecordType);
        assertEquals(Long.valueOf(2), readRecordType.getVersion());
        assertNull(readRecordType.getFieldTypeEntry(fieldType1.getId()));
    }

    @Test
    public void testRecordTypeBuilderFieldsAndSupertypes() throws Exception {
        String NS = "testRecordTypeBuilderFieldsAndSupertypes";

        //
        // Create some field types
        //
        FieldType field1 = typeManager.createFieldType("STRING", new QName(NS, "field1"), VERSIONED);
        FieldType field2 = typeManager.createFieldType("STRING", new QName(NS, "field2"), VERSIONED);
        FieldType field3 = typeManager.createFieldType("STRING", new QName(NS, "field3"), VERSIONED);
        FieldType field4 = typeManager.createFieldType("STRING", new QName(NS, "field4"), VERSIONED);
        FieldType field5 = typeManager.createFieldType("STRING", new QName(NS, "field5"), VERSIONED);

        //
        // Create some supertypes
        //
        FieldType field21 = typeManager.createFieldType("STRING", new QName(NS, "field21"), VERSIONED);
        FieldType field22 = typeManager.createFieldType("STRING", new QName(NS, "field22"), VERSIONED);
        FieldType field23 = typeManager.createFieldType("STRING", new QName(NS, "field23"), VERSIONED);
        FieldType field24 = typeManager.createFieldType("STRING", new QName(NS, "field24"), VERSIONED);
        FieldType field25 = typeManager.createFieldType("STRING", new QName(NS, "field25"), VERSIONED);
        FieldType field26 = typeManager.createFieldType("STRING", new QName(NS, "field26"), VERSIONED);
        FieldType field27 = typeManager.createFieldType("STRING", new QName(NS, "field27"), VERSIONED);
        FieldType field28 = typeManager.createFieldType("STRING", new QName(NS, "field28"), VERSIONED);
        FieldType field29 = typeManager.createFieldType("STRING", new QName(NS, "field29"), VERSIONED);

        RecordType supertype1 = typeManager.recordTypeBuilder().name(NS, "supertype1").fieldEntry().use(field21).add().create();
        RecordType supertype2 = typeManager.recordTypeBuilder().name(NS, "supertype2").fieldEntry().use(field22).add().create();
        RecordType supertype3 = typeManager.recordTypeBuilder().name(NS, "supertype3").fieldEntry().use(field23).add().create();
        RecordType supertype4 = typeManager.recordTypeBuilder().name(NS, "supertype4").fieldEntry().use(field24).add().create();
        RecordType supertype5 = typeManager.recordTypeBuilder().name(NS, "supertype5").fieldEntry().use(field25).add().create();
        RecordType supertype6 = typeManager.recordTypeBuilder().name(NS, "supertype6").fieldEntry().use(field26).add().create();
        RecordType supertype7 = typeManager.recordTypeBuilder().name(NS, "supertype7").fieldEntry().use(field27).add().create();
        // give supertype7 two more versions
        supertype7.addFieldTypeEntry(field28.getId(), false);
        supertype7 = typeManager.updateRecordType(supertype7);
        supertype7.addFieldTypeEntry(field29.getId(), false);
        supertype7 = typeManager.updateRecordType(supertype7);

        RecordType recordType = typeManager
                .recordTypeBuilder()
                .defaultNamespace(NS)
                .name("recordType1")

                /* Adding previously defined fields */
                /* By ID */
                .fieldEntry().id(field1.getId()).add()
                /* By object + test mandatory flag */
                .fieldEntry().use(field2).mandatory().add()
                /* By non-qualified name */
                .fieldEntry().name("field3").add()
                /* By qualified name */
                .fieldEntry().name(new QName(NS, "field4")).add()
                /* By indirect qualified name*/
                .fieldEntry().name(NS, "field5").add()

                /* Adding newly created fields */
                /* Using default default scope */
                .fieldEntry().defineField().name("field10").type("LIST<STRING>").create().add()
                /* Using default type (STRING) */
                .fieldEntry().defineField().name("field11").create().add()
                /* Using QName */
                .fieldEntry().defineField().name(new QName(NS, "field12")).create().add()
                /* Using explicit scope */
                .fieldEntry().defineField().name("field13").type("LONG").scope(VERSIONED).create().add()
                /* Using different default scope */
                .defaultScope(Scope.VERSIONED)
                .fieldEntry().defineField().name("field14").create().add()
                /* Using indirect qualified name*/
                .fieldEntry().defineField().name(NS, "field15").create().add()

                /* Adding supertypes */
                .supertype().id(supertype1.getId()).add()
                .supertype().name("supertype2").add()
                .supertype().name(new QName(NS, "supertype3")).add()
                .supertype().name(NS, "supertype4").add()
                .supertype().use(supertype5).add()
                .supertype().name(NS, "supertype7").version(2L).add()

                .create();

        //
        // Global checks
        //
        assertEquals(new QName(NS, "recordType1"), recordType.getName());

        //
        // Verify fields
        //
        assertEquals(11, recordType.getFieldTypeEntries().size());

        assertFalse(recordType.getFieldTypeEntry(field1.getId()).isMandatory());
        assertTrue(recordType.getFieldTypeEntry(field2.getId()).isMandatory());
        assertFalse(recordType.getFieldTypeEntry(field3.getId()).isMandatory());

        // Verify the inline created fields
        FieldType field10 = typeManager.getFieldTypeByName(new QName(NS, "field10"));
        assertEquals("LIST<STRING>", field10.getValueType().getName());
        assertEquals(Scope.NON_VERSIONED, field10.getScope());
        assertNotNull(recordType.getFieldTypeEntry(field10.getId()));

        FieldType field11 = typeManager.getFieldTypeByName(new QName(NS, "field11"));
        assertEquals("STRING", field11.getValueType().getName());
        assertEquals(Scope.NON_VERSIONED, field11.getScope());
        assertNotNull(recordType.getFieldTypeEntry(field11.getId()));

        FieldType field13 = typeManager.getFieldTypeByName(new QName(NS, "field13"));
        assertEquals(Scope.VERSIONED, field13.getScope());

        FieldType field14 = typeManager.getFieldTypeByName(new QName(NS, "field14"));
        assertEquals(Scope.VERSIONED, field14.getScope());

        //
        // Verify supertypes
        //
        Map<SchemaId, Long> supertypes = recordType.getSupertypes();
        assertEquals(6, supertypes.size());
        assertTrue(supertypes.containsKey(supertype1.getId()));
        assertTrue(supertypes.containsKey(supertype2.getId()));
        assertTrue(supertypes.containsKey(supertype3.getId()));
        assertTrue(supertypes.containsKey(supertype4.getId()));
        assertTrue(supertypes.containsKey(supertype5.getId()));
        assertFalse(supertypes.containsKey(supertype6.getId()));
        assertTrue(supertypes.containsKey(supertype7.getId()));

        assertEquals(new Long(1), supertypes.get(supertype1.getId()));
        assertEquals(new Long(2), supertypes.get(supertype7.getId()));
    }

    @Test
    public void testRecordTypeBuilderCreateOrUpdate() throws Exception {
        String NS = "testRecordTypeBuilderCreateOrUpdate";

        RecordType recordType = null;
        for (int i = 0; i < 3; i++) {
            recordType = typeManager
                    .recordTypeBuilder()
                    .defaultNamespace(NS)
                    .name("recordType1")
                    .fieldEntry().defineField().name("field1").createOrUpdate().add()
                    .fieldEntry().defineField().name("field2").createOrUpdate().add()
                    .createOrUpdate();
        }

        assertEquals(new Long(1L), recordType.getVersion());

        recordType = typeManager
                .recordTypeBuilder()
                .defaultNamespace(NS)
                .name("recordType1")
                .fieldEntry().defineField().name("field1").createOrUpdate().add()
                .fieldEntry().defineField().name("field2").createOrUpdate().add()
                .fieldEntry().defineField().name("field3").createOrUpdate().add()
                .createOrUpdate();

        assertEquals(new Long(2L), recordType.getVersion());
    }

    @Test
    public void testRefreshSubtypes() throws Exception {
        // The following code creates this type hierarchy:
        //
        //     rtA
        //      | \
        //     rtB rtD
        //      | /
        //     rtC
        //

        RecordType rtA = typeManager.recordTypeBuilder()
                .name("RefreshSubtypes", "rtA")
                .fieldEntry().use(fieldType1).add()
                .create();

        RecordType rtB = typeManager.recordTypeBuilder()
                .name("RefreshSubtypes", "rtB")
                .fieldEntry().use(fieldType1).add()
                .supertype().use(rtA).add()
                .create();

        RecordType rtD = typeManager.recordTypeBuilder()
                .name("RefreshSubtypes", "rtD")
                .fieldEntry().use(fieldType1).add()
                .supertype().use(rtA).add()
                .create();

        RecordType rtC = typeManager.recordTypeBuilder()
                .name("RefreshSubtypes", "rtC")
                .fieldEntry().use(fieldType1).add()
                .supertype().use(rtB).add()
                .supertype().use(rtD).add()
                .create();

        // Check currently the all point to the first version of their supertype
        assertEquals(Long.valueOf(1L), rtD.getSupertypes().get(rtA.getId()));
        assertEquals(Long.valueOf(1L), rtB.getSupertypes().get(rtA.getId()));
        assertEquals(Long.valueOf(1L), rtC.getSupertypes().get(rtB.getId()));
        assertEquals(Long.valueOf(1L), rtC.getSupertypes().get(rtD.getId()));

        // Update record type B, pointer in record type C should be updated
        rtB.addFieldTypeEntry(fieldType2.getId(), false);
        rtB = typeManager.updateRecordType(rtB, true);

        // Now C should point to new version of B
        waitOnRecordTypeVersion(2L, rtC.getId());
        rtC = typeManager.getRecordTypeById(rtC.getId(), null);
        assertEquals(Long.valueOf(2L), rtC.getSupertypes().get(rtB.getId()));
        // And thus C itself should have two versions
        assertEquals(Long.valueOf(2L), rtC.getVersion());

        // Update record type A, this should cause updates to B, C and D
        rtA.addFieldTypeEntry(fieldType2.getId(), false);
        rtA = typeManager.updateRecordType(rtA, true);

        // Check the subtypes were updated
        waitOnRecordTypeVersion(3L, rtB.getId());
        rtB = typeManager.getRecordTypeById(rtB.getId(), null);
        assertEquals(Long.valueOf(3L), rtB.getVersion());
        assertEquals(Long.valueOf(2L), rtB.getSupertypes().get(rtA.getId()));

        waitOnRecordTypeVersion(2L, rtD.getId());
        rtD = typeManager.getRecordTypeById(rtD.getId(), null);
        assertEquals(Long.valueOf(2L), rtD.getVersion());
        assertEquals(Long.valueOf(2L), rtD.getSupertypes().get(rtA.getId()));

        waitOnRecordTypeVersion(4L, rtC.getId());
        rtC = typeManager.getRecordTypeById(rtC.getId(), null);
        assertEquals(Long.valueOf(4L), rtC.getVersion());
        assertEquals(Long.valueOf(3L), rtC.getSupertypes().get(rtB.getId()));
        assertEquals(Long.valueOf(2L), rtC.getSupertypes().get(rtD.getId()));
    }
    
    @Test
    public void testGetFieldTypesForRecordType_WithRecursion() throws TypeException, RepositoryException, InterruptedException {
        RecordType rtA = typeManager.recordTypeBuilder()
                .name("GetFieldTypes", "rtA")
                .fieldEntry().use(fieldType1).add()
                .create();

        RecordType rtB = typeManager.recordTypeBuilder()
                .name("GetFieldTypes", "rtB")
                .fieldEntry().use(fieldType2).add()
                .supertype().use(rtA).add()
                .create();
        
        RecordType rtC = typeManager.recordTypeBuilder()
                .name("GetFieldTypes", "rtC")
                .fieldEntry().use(fieldType3).add()
                .supertype().use(rtB).add()
                .create();
        
        verifyFieldTypes(Sets.newHashSet(fieldType1.getId()),
                typeManager.getFieldTypesForRecordType(rtA, true));
        
        verifyFieldTypes(Sets.newHashSet(fieldType1.getId(), fieldType2.getId()),
                typeManager.getFieldTypesForRecordType(rtB, true));
        
        verifyFieldTypes(Sets.newHashSet(fieldType1.getId(), fieldType2.getId(), fieldType3.getId()),
                typeManager.getFieldTypesForRecordType(rtC, true));
        
        verifyFieldTypes(Sets.newHashSet(fieldType3.getId()),
                typeManager.getFieldTypesForRecordType(rtC, false));
        
        
    }
    
    private void verifyFieldTypes(Set<SchemaId> expectedSchemaIds, Collection<FieldTypeEntry> actualFieldTypes) {
        assertEquals(expectedSchemaIds.size(), actualFieldTypes.size());
        Set<SchemaId> actualSchemaIds = Sets.newHashSet();
        for (FieldTypeEntry fieldTypeEntry : actualFieldTypes) {
            actualSchemaIds.add(fieldTypeEntry.getFieldTypeId());
        }
        assertEquals(expectedSchemaIds, actualSchemaIds);
    }

    protected void waitOnRecordTypeVersion(long version, SchemaId recordTypeId)
            throws InterruptedException, RepositoryException {
        // do nothing
    }

}
