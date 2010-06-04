package org.lilycms.indexer.conf;

import java.util.*;

// TODO for safety should consider making some of the returned lists immutable

/**
 * The configuration for the indexer, describes how record types should be mapped
 * onto index documents.
 *
 * <p>Fields and vtags are identified by ID in this object model.
 */
public class IndexerConf {
    private List<IndexCase> indexCases = new ArrayList<IndexCase>();
    private List<IndexField> indexFields = new ArrayList<IndexField>();
    private Set<String> repoFieldDependencies = new HashSet<String>();
    private List<IndexField> derefIndexFields = new ArrayList<IndexField>();
    private Map<String, List<IndexField>> derefIndexFieldsByField = new HashMap<String, List<IndexField>>();
    private Set<String> vtags = new HashSet<String>();

    protected void addIndexCase(IndexCase indexCase) {
        indexCases.add(indexCase);
        vtags.addAll(indexCase.getVersionTags());
    }

    /**
     * @return null if there is no matching IndexCase
     */
    public IndexCase getIndexCase(String recordTypeName, Map<String, String> varProps) {
        for (IndexCase indexCase : indexCases) {
            if (indexCase.match(recordTypeName, varProps)) {
                return indexCase;
            }
        }

        return null;
    }

    public List<IndexField> getIndexFields() {
        return indexFields;
    }

    protected void addIndexField(IndexField indexField) {
        indexFields.add(indexField);

        String fieldDep = indexField.getValue().getFieldDependency();
        if (fieldDep != null)
            repoFieldDependencies.add(fieldDep);

        if (indexField.getValue() instanceof DerefValue) {
            derefIndexFields.add(indexField);

            String fieldId = ((DerefValue)indexField.getValue()).getTargetField().getId();
            List<IndexField> fields = derefIndexFieldsByField.get(fieldId);
            if (fields == null) {
                fields = new ArrayList<IndexField>();
                derefIndexFieldsByField.put(fieldId, fields);
            }
            fields.add(indexField);
        }
    }

    /**
     * Checks if the supplied field type is used by one of the indexField's.
     */
    public boolean isIndexFieldDependency(String fieldTypeId) {
        return repoFieldDependencies.contains(fieldTypeId);
    }

    /**
     * Returns all IndexField's which have a DerefValue.
     */
    public List<IndexField> getDerefIndexFields() {
        return derefIndexFields;
    }

    /**
     * Returns all IndexFields which have a DerefValue pointing to the given field id, or null if there are none.
     */
    public List<IndexField> getDerefIndexFields(String fieldId) {
        List<IndexField> result = derefIndexFieldsByField.get(fieldId);
        return result == null ? Collections.<IndexField>emptyList() : result;
    }

    /**
     * Returns the set of all known vtags, thus all the vtags that are relevant to indexing.
     */
    public Set<String> getVtags() {
        return vtags;
    }
}
