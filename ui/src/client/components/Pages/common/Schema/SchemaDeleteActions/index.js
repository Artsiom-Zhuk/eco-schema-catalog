/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import { connect } from 'react-redux';
import { showConfirmWindow } from '../../../../../actions/modalActions/modalActions';
import { deleteSchemaActionAsync, deleteSchemasActionAsync } from '../../../../../actions/schemaActions/schemaActions';
import SchemaDeleteActions from './SchemaDeleteActions';

const mapDispatchToProps = dispatch => ({
  deleteSchema: () => dispatch(showConfirmWindow(
    true,
    () => dispatch(deleteSchemaActionAsync()),
    'Are you really want to delete that schema version?',
  )),
  deleteSchemas: () => dispatch(showConfirmWindow(
    true,
    () => dispatch(deleteSchemasActionAsync()),
    'Are you really want to delete all schema versions?',
  )),
});

export default (connect(null, mapDispatchToProps)(SchemaDeleteActions));
