/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.api;

import org.neo4j.graphalgo.NodeLabel;
import org.neo4j.graphalgo.RelationshipType;
import org.neo4j.graphalgo.config.AlgoBaseConfig;
import org.neo4j.graphalgo.config.ConfigurableSeedConfig;
import org.neo4j.graphalgo.config.FeaturePropertiesConfig;
import org.neo4j.graphalgo.config.MutatePropertyConfig;
import org.neo4j.graphalgo.config.MutateRelationshipConfig;
import org.neo4j.graphalgo.config.NodeWeightConfig;
import org.neo4j.graphalgo.config.RelationshipWeightConfig;
import org.neo4j.graphalgo.config.SeedConfig;
import org.neo4j.graphalgo.core.loading.GraphStoreWithConfig;
import org.neo4j.graphalgo.utils.StringJoining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.neo4j.graphalgo.utils.StringFormatting.formatWithLocale;

public final class GraphStoreValidation {

    public static <CONFIG extends AlgoBaseConfig> void validate(GraphStoreWithConfig graphStoreWithConfig, CONFIG config) {
        GraphStore graphStore = graphStoreWithConfig.graphStore();

        Collection<NodeLabel> filterLabels = config.nodeLabelIdentifiers(graphStore);
        if (config instanceof SeedConfig) {
            String seedProperty = ((SeedConfig) config).seedProperty();
            if (seedProperty != null && !graphStore.hasNodeProperty(filterLabels, seedProperty)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Seed property `%s` not found in graph with node properties: %s",
                    seedProperty,
                    graphStore.nodePropertyKeys().values()
                ));
            }
        }
        if (config instanceof ConfigurableSeedConfig) {
            ConfigurableSeedConfig configurableSeedConfig = (ConfigurableSeedConfig) config;
            String seedProperty = configurableSeedConfig.seedProperty();
            if (seedProperty != null && !graphStore.hasNodeProperty(filterLabels, seedProperty)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "`%s`: `%s` not found in graph with node properties: %s",
                    configurableSeedConfig.propertyNameOverride(),
                    seedProperty,
                    graphStore.nodePropertyKeys().values()
                ));
            }
        }
        if (config instanceof FeaturePropertiesConfig) {
            var nodePropertiesConfig = (FeaturePropertiesConfig) config;
            List<String> weightProperties = nodePropertiesConfig.featureProperties();
            List<String> missingProperties;
            if (nodePropertiesConfig.propertiesMustExistForEachNodeLabel()) {
                 missingProperties = weightProperties
                    .stream()
                    .filter(weightProperty -> !graphStore.hasNodeProperty(filterLabels, weightProperty))
                    .collect(Collectors.toList());
            } else {
                var availableProperties = filterLabels.stream().flatMap(label -> graphStore.nodePropertyKeys(label).stream()).collect(Collectors.toSet());
                missingProperties = new ArrayList<>(weightProperties);
                missingProperties.removeAll(availableProperties);
            }
            if (!missingProperties.isEmpty()) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Node properties %s not found in graph with node properties: %s in all node labels: %s",
                    missingProperties,
                    graphStore.nodePropertyKeys(filterLabels),
                    StringJoining.join(filterLabels.stream().map(NodeLabel::name))
                ));
            }
        }
        if (config instanceof NodeWeightConfig) {
            String weightProperty = ((NodeWeightConfig) config).nodeWeightProperty();
            if (weightProperty != null && !graphStore.hasNodeProperty(filterLabels, weightProperty)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Node weight property `%s` not found in graph with node properties: %s in all node labels: %s",
                    weightProperty,
                    graphStore.nodePropertyKeys(filterLabels),
                    StringJoining.join(filterLabels.stream().map(NodeLabel::name))
                ));
            }
        }
        if (config instanceof RelationshipWeightConfig) {
            String weightProperty = ((RelationshipWeightConfig) config).relationshipWeightProperty();
            Collection<RelationshipType> internalRelationshipTypes = config.internalRelationshipTypes(graphStore);
            if (weightProperty != null && !graphStore.hasRelationshipProperty(internalRelationshipTypes, weightProperty)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Relationship weight property `%s` not found in graph with relationship properties: %s in all relationship types: %s",
                    weightProperty,
                    graphStore.relationshipPropertyKeys(internalRelationshipTypes),
                    StringJoining.join(internalRelationshipTypes.stream().map(RelationshipType::name))
                ));
            }
        }

        if (config instanceof MutatePropertyConfig) {
            MutatePropertyConfig mutateConfig = (MutatePropertyConfig) config;
            String mutateProperty = mutateConfig.mutateProperty();

            if (mutateProperty != null && graphStore.hasNodeProperty(filterLabels, mutateProperty)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Node property `%s` already exists in the in-memory graph.",
                    mutateProperty
                ));
            }
        }

        if (config instanceof MutateRelationshipConfig) {
            String mutateRelationshipType = ((MutateRelationshipConfig) config).mutateRelationshipType();
            if (mutateRelationshipType != null && graphStore.hasRelationshipType(RelationshipType.of(mutateRelationshipType))) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Relationship type `%s` already exists in the in-memory graph.",
                    mutateRelationshipType
                ));
            }
        }
    }

    private GraphStoreValidation() {}
}
