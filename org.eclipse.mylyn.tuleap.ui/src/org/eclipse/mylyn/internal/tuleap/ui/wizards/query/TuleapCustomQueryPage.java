/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.tuleap.ui.wizards.query;

import java.util.List;

import org.eclipse.mylyn.commons.workbench.forms.SectionComposite;
import org.eclipse.mylyn.internal.tuleap.core.client.TuleapClient;
import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapFormElement;
import org.eclipse.mylyn.internal.tuleap.core.model.AbstractTuleapStructuralElement;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapMultiSelectBox;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapSelectBox;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapString;
import org.eclipse.mylyn.internal.tuleap.core.model.field.TuleapText;
import org.eclipse.mylyn.internal.tuleap.core.model.structural.TuleapFieldSet;
import org.eclipse.mylyn.internal.tuleap.core.repository.TuleapRepositoryConfiguration;
import org.eclipse.mylyn.internal.tuleap.core.repository.TuleapRepositoryConnector;
import org.eclipse.mylyn.internal.tuleap.ui.TuleapTasksUIPlugin;
import org.eclipse.mylyn.internal.tuleap.ui.util.ITuleapUIConstants;
import org.eclipse.mylyn.internal.tuleap.ui.util.TuleapMylynTasksUIMessages;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The second page of the Tuleap query wizard with the form based search page.
 * 
 * @author <a href="mailto:stephane.begaudeau@obeo.fr">Stephane Begaudeau</a>
 * @since 1.0
 */
public class TuleapCustomQueryPage extends AbstractRepositoryQueryPage2 {

	/**
	 * The constructor.
	 * 
	 * @param taskRepository
	 *            The Mylyn task repository
	 */
	public TuleapCustomQueryPage(TaskRepository taskRepository) {
		this(taskRepository, null);
	}

	/**
	 * The constructor.
	 * 
	 * @param taskRepository
	 *            The Mylyn task repository
	 * @param queryToEdit
	 *            The query to edit
	 */
	public TuleapCustomQueryPage(TaskRepository taskRepository, IRepositoryQuery queryToEdit) {
		super(TuleapMylynTasksUIMessages.getString("TuleapCustomQueryPage.Name"), taskRepository, queryToEdit); //$NON-NLS-1$
		this.setTitle(TuleapMylynTasksUIMessages.getString("TuleapCustomQueryPage.Title")); //$NON-NLS-1$
		this.setDescription(TuleapMylynTasksUIMessages.getString("TuleapCustomQueryPage.Description")); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2#doRefreshControls()
	 */
	@Override
	protected void doRefreshControls() {
		// nothing yet
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2#hasRepositoryConfiguration()
	 */
	@Override
	protected boolean hasRepositoryConfiguration() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2#restoreState(org.eclipse.mylyn.tasks.core.IRepositoryQuery)
	 */
	@Override
	protected boolean restoreState(IRepositoryQuery query) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage#applyTo(org.eclipse.mylyn.tasks.core.IRepositoryQuery)
	 */
	@Override
	public void applyTo(IRepositoryQuery query) {
		query.setUrl(this.getQueryURL(this.getTaskRepository(), this.getQueryParameters()));
		query.setSummary(this.getQueryTitle());
	}

	/**
	 * Returns the beginning of the query parameters.
	 * 
	 * @return The beginning of the query parameters.
	 */
	private StringBuilder getQueryParameters() {
		return new StringBuilder("name=allTuleapTasks");
	}

	/**
	 * Returns the start of the url of the repository.
	 * 
	 * @param repository
	 *            The Mylyn task repository.
	 * @return The start of the url of the repository.
	 */
	private StringBuilder getQueryURLStart(TaskRepository repository) {
		StringBuilder sb = new StringBuilder(repository.getRepositoryUrl());

		if (sb.charAt(sb.length() - 1) != '/') {
			sb.append('/');
		}
		sb.append("buglist.cgi?"); //$NON-NLS-1$
		return sb;
	}

	/**
	 * Returns the url of the query.
	 * 
	 * @param repository
	 *            The Mylyn tasks repository
	 * @param params
	 *            the parameters of the query
	 * @return The url of the query
	 */
	private String getQueryURL(TaskRepository repository, StringBuilder params) {
		StringBuilder url = new StringBuilder(this.getQueryURLStart(repository).toString());
		url.append(params);
		return url.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2#createPageContent(org.eclipse.mylyn.commons.workbench.forms.SectionComposite)
	 */
	@Override
	protected void createPageContent(SectionComposite parent) {
		// TODO Creates the custom query page
		String connectorKind = this.getTaskRepository().getConnectorKind();
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				connectorKind);
		if (connector instanceof TuleapRepositoryConnector) {
			TuleapRepositoryConnector repositoryConnector = (TuleapRepositoryConnector)connector;
			TuleapClient client = repositoryConnector.getClientManager().getClient(this.getTaskRepository());
			TuleapRepositoryConfiguration repositoryConfiguration = repositoryConnector
					.getRepositoryConfiguration(this.getTaskRepository().getRepositoryUrl());
			if (repositoryConfiguration != null) {
				List<AbstractTuleapStructuralElement> formElements = repositoryConfiguration
						.getFormElements();
				for (AbstractTuleapStructuralElement abstractTuleapStructuralElement : formElements) {
					if (abstractTuleapStructuralElement instanceof TuleapFieldSet) {
						// Create a SWT group for each Tuleap field set
						TuleapFieldSet fieldSet = (TuleapFieldSet)abstractTuleapStructuralElement;

						Group group = new Group(parent.getContent(), SWT.NONE);
						group.setText(fieldSet.getLabel());
						GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
						gridData.grabExcessVerticalSpace = false;
						group.setLayoutData(gridData);
						group.setLayout(new GridLayout(3, false));

						List<AbstractTuleapFormElement> fieldSetFormElements = fieldSet.getFormElements();
						for (AbstractTuleapFormElement abstractTuleapFormElement : fieldSetFormElements) {
							if (abstractTuleapFormElement instanceof TuleapString) {
								// Create a single line text field for each Tuleap string
								TuleapString tuleapString = (TuleapString)abstractTuleapFormElement;
								Label label = new Label(group, SWT.NONE);
								label.setText(tuleapString.getLabel());

								Combo combo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
								combo.setItems(new String[] {"contains", "equals", "starts with",
										"ends with", });
								combo.setText("contains");

								Text text = new Text(group, SWT.SINGLE | SWT.BORDER);
								gridData = new GridData(GridData.FILL_HORIZONTAL);
								text.setLayoutData(gridData);
							} else if (abstractTuleapFormElement instanceof TuleapSelectBox) {
								// Create a single selection combo for each Tuleap select box
								TuleapSelectBox tuleapSelectBox = (TuleapSelectBox)abstractTuleapFormElement;
								Label label = new Label(group, SWT.NONE);
								label.setText(tuleapSelectBox.getLabel());

								Combo combo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
								combo.setItems(new String[] {"equals", });
								combo.setText("equals");

								combo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
								combo.setItems(tuleapSelectBox.getItems().toArray(
										new String[tuleapSelectBox.getItems().size()]));
								gridData = new GridData(GridData.FILL_HORIZONTAL);
								combo.setLayoutData(gridData);
								if (tuleapSelectBox.getItems().size() > 0) {
									combo.setText(tuleapSelectBox.getItems().get(0));
								}
							} else if (abstractTuleapFormElement instanceof TuleapMultiSelectBox) {
								// Create a multi selection combo for each Tuleap multi select box
								TuleapMultiSelectBox tuleapSelectBox = (TuleapMultiSelectBox)abstractTuleapFormElement;
								Label label = new Label(group, SWT.NONE);
								label.setText(tuleapSelectBox.getLabel());

								Combo combo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
								combo.setItems(new String[] {"contains", "equals", });
								combo.setText("contains");

								org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(group,
										SWT.MULTI | SWT.READ_ONLY | SWT.BORDER);
								list.setItems(tuleapSelectBox.getItems().toArray(
										new String[tuleapSelectBox.getItems().size()]));
								gridData = new GridData(GridData.FILL_HORIZONTAL);
								list.setLayoutData(gridData);
							} else if (abstractTuleapFormElement instanceof TuleapText) {
								// Create a multi line text for each Tuleap text
								TuleapText tuleapText = (TuleapText)abstractTuleapFormElement;
								Label label = new Label(group, SWT.NONE);
								label.setText(tuleapText.getLabel());

								Combo combo = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
								combo.setItems(new String[] {"contains", "equals", "starts with",
										"ends with", });
								combo.setText("contains");

								Text text = new Text(group, SWT.BORDER | SWT.MULTI);
								gridData = new GridData(GridData.FILL_HORIZONTAL);
								text.setLayoutData(gridData);
							}
						}
					}
				}
			}
		} else {
			throw new IllegalArgumentException(TuleapMylynTasksUIMessages.getString(
					"TuleapCustomQueryPage.InvalidConnector", this.getTaskRepository().getRepositoryUrl()));
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#getImage()
	 */
	@Override
	public Image getImage() {
		return TuleapTasksUIPlugin.getDefault().getImage(ITuleapUIConstants.Icons.WIZARD_TULEAP_LOGO_48);
	}

}