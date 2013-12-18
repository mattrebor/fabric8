/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.itests.basic.examples;


import org.apache.curator.framework.CuratorFramework;
import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Arrays;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-673] Fix fabric basic ExampleMQProfileTest")
public class ExampleMQProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testExample() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        CuratorFramework curator = getCurator();
        Set<Container> containers = ContainerBuilder.create(2).withName("cnt").withProfiles("default").assertProvisioningResult().build();
        Container broker = containers.iterator().next();
        containers.remove(broker);

        setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(broker.getId()), "changing");
        System.err.println(executeCommand("fabric:container-change-profile " + broker.getId() + " mq-default"));
        Provision.provisioningSuccess(Arrays.asList(new Container[]{broker}), PROVISION_TIMEOUT);
        System.err.println(executeCommand("fabric:cluster-list"));

        for(Container c : containers) {
            setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing");
            System.err.println(executeCommand("fabric:container-change-profile " + c.getId() + " example-mq"));
        }
        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);
        System.err.println(executeCommand("fabric:cluster-list"));

        assertTrue(Provision.waitForCondition(Arrays.asList(new Container[]{broker}), new ContainerCondition() {
            @Override
            public Boolean checkConditionOnContainer(final Container c) {
                System.err.println(executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" activemq:bstat"));
                String output = executeCommand("fabric:container-connect -u admin -p admin "+c.getId()+" activemq:query -QQueue=FABRIC.DEMO");
                return output.contains("DequeueCount = ") && !output.contains("DequeueCount = 0");
            }
        }, 10000L));
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}