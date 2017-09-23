package org.datadozer.index

import org.datadozer.LuceneDocument
import org.datadozer.index.fields.FieldSchema
import org.datadozer.index.fields.FieldTemplate
import org.datadozer.index.fields.IdField
import org.datadozer.models.Document
import org.datadozer.models.FieldValue

/*
 * Licensed to DataDozer under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. DataDozer licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

private data class BuilderFields(val field: FieldTemplate, val schema: FieldSchema) {
    fun updateField(value: FieldValue) {
        schema.fieldType.updateDocument(value, schema, field)
    }
}

class DocumentBuilder private constructor(
        private val fields: HashMap<String, BuilderFields>,
        private val document: LuceneDocument) {

    companion object Builder {
        fun build(setting: IndexSettings): DocumentBuilder {
            val template = LuceneDocument()
            val fields = HashMap<String, BuilderFields>()

            // Make sure we skip the first few fields till we reach the number of metadata fields.
            for (f in setting.fields) {
                fields.put(f.schemaName, BuilderFields(f.fieldType.createFieldTemplate(f), f))
            }

            return DocumentBuilder(fields, template)
        }
    }

    /**
     * Update the lucene Document based upon the passed Datadozer Document.
     * Note: Do not update the document from multiple threads.
     */
    fun updateDocument(document: Document): LuceneDocument {
        // Make sure that there are no items in the documents
        this.document.clear()

        fields[IdField.SCHEMA.schemaName]!!.updateField(document.id)
        //fields[ModifyIndex.SCHEMA.schemaName]!!.schema.fieldType.updateDocument(FieldValue.newBuilder().setLongValue(document))

        for ((key, fieldValue) in document.fieldsMap) {
            this.fields[key]?.updateField(fieldValue)
        }

        return this.document
    }
}