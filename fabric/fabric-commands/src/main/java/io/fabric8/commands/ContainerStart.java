/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.commands;

import java.util.Collection;

import org.apache.felix.gogo.commands.Command;

import io.fabric8.api.Container;
import static io.fabric8.utils.FabricValidations.validateContainersName;

@Command(name = "container-start", scope = "fabric", description = "Start the specified container", detailedDescription = "classpath:containerStart.txt")
public class ContainerStart extends ContainerLifecycleCommand {

    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        Collection<String> expandedNames = super.expandGlobNames(containers);
        for (String containerName: expandedNames) {
            validateContainersName(containerName);
            Container found = getContainer(containerName);
            applyUpdatedCredentials(found);
            if (force || !found.isAlive()) {
                found.start(force);
            } else {
                System.err.println("Container " + containerName + " is already started");
            }
        }
        return null;
    }

}
