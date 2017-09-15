package org.datadozer.index

import org.datadozer.LuceneDocument
import org.datadozer.models.Document

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

class DocumentTemplate private constructor(
        private val setting: IndexSettings,
        private val templateFields: Array<FieldTemplate>,
        private val template: LuceneDocument,
        private val metaDataFieldCount: Int) {

    companion object Builder {
        fun build(setting: IndexSettings): DocumentTemplate {
            val template = LuceneDocument()
            val fields = ArrayList<FieldTemplate>()

            fun add(field: FieldTemplate) {
                template.add(field.fields[0])
                if (field.docValues != null) {
                    template.add(field.docValues[0])
                }
                fields.add(field)
            }

            // Add the two mandatory internal field
            add(IdField.INSTANCE.createFieldTemplate(IdField.SCHEMA))
            add(TimeStampField.INSTANCE.createFieldTemplate(TimeStampField.SCHEMA))
            val metaDataFieldCount = 2
            // Make sure we skip the first few fields till we reach the number of metadata field.
            for (f in setting.fields.drop(metaDataFieldCount)) {
                add(f.fieldType.createFieldTemplate(f))
            }

            return DocumentTemplate(setting, fields.toTypedArray(), template, metaDataFieldCount)
        }
    }

    /**
     * Update the lucene Document based upon the passed Datadozer Document.
     * Note: Do not update the document from multiple threads.
     */
    fun updateDocument(document: Document): LuceneDocument {
        for (i in 0 until document.fieldsCount) {
            val fieldSchema = setting.fields.get(i)
            val fieldTemplate = templateFields[i]
            fieldSchema.fieldType.updateDocument(document, fieldSchema, fieldTemplate)
        }

        return template
    }
}