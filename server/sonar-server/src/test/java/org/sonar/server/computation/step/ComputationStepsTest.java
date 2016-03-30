/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import java.util.Set;
import org.junit.Test;
import org.picocontainer.ComponentAdapter;
import org.sonar.ce.queue.CeTask;
import org.sonar.ce.settings.ThreadLocalSettings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.ContainerPopulator;
import org.sonar.server.computation.container.ComputeEngineContainerImpl;
import org.sonar.server.computation.container.ReportComputeEngineContainerPopulator;
import org.sonar.server.computation.container.StepsExplorer;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.difference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ComputationStepsTest {

  @Test
  public void fail_if_a_step_is_not_registered_in_picocontainer() {
    try {
      Lists.newArrayList(new ReportComputationSteps(new ComputeEngineContainerImpl(new ComponentContainer(), mock(ContainerPopulator.class))).instances());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Component not found");
    }
  }

  @Test
  public void all_steps_from_package_step_are_present_in_container() {
    ComponentContainer parent = new ComponentContainer();
    parent.add(mock(ThreadLocalSettings.class));
    ComputeEngineContainerImpl ceContainer = new ComputeEngineContainerImpl(parent, new ReportComputeEngineContainerPopulator(mock(CeTask.class), null));

    Set<String> stepsCanonicalNames = StepsExplorer.retrieveStepPackageStepsCanonicalNames();

    Set<String> typesInContainer = from(ceContainer.getPicoContainer().getComponentAdapters())
        .transform(ComponentAdapterToImplementationClass.INSTANCE)
        .filter(IsComputationStep.INSTANCE)
        .transform(StepsExplorer.toCanonicalName())
        .toSet();

    // PersistDevelopersStep is the only step that is not in the report container (it's only added when Dev Cockpit plugin is installed);
    assertThat(difference(stepsCanonicalNames, typesInContainer)).containsOnly(PersistDevelopersStep.class.getCanonicalName());
  }

  private enum ComponentAdapterToImplementationClass implements Function<ComponentAdapter<?>, Class<?>> {
    INSTANCE;

    @Override
    public Class<?> apply(ComponentAdapter<?> input) {
      return input.getComponentImplementation();
    }
  }

  private enum IsComputationStep implements Predicate<Class<?>> {
    INSTANCE;

    @Override
    public boolean apply(Class<?> input) {
      return ComputationStep.class.isAssignableFrom(input);
    }
  }
}
