package io.microconfig.commands.factory;

import io.microconfig.commands.BuildPropertiesCommand;
import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.environments.filebased.EnvironmentParserImpl;
import io.microconfig.environments.filebased.FileBasedEnvironmentProvider;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.files.parser.FileComponentParser;
import io.microconfig.properties.files.provider.ComponentTree;
import io.microconfig.properties.files.provider.ComponentTreeCache;
import io.microconfig.properties.files.provider.FileBasedPropertiesProvider;
import io.microconfig.properties.resolver.PropertyFetcherImpl;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.ResolvedPropertiesProvider;
import io.microconfig.properties.resolver.placeholder.PlaceholderResolver;
import io.microconfig.properties.resolver.specific.EnvSpecificPropertiesProvider;
import io.microconfig.properties.resolver.spel.SpelExpressionResolver;
import io.microconfig.properties.serializer.PropertiesDiffWriter;
import io.microconfig.properties.serializer.PropertiesSerializerImpl;
import io.microconfig.properties.serializer.PropertySerializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.commands.PropertiesPostProcessor.emptyPostProcessor;
import static io.microconfig.utils.CacheHandler.cache;
import static io.microconfig.utils.FileUtils.canonical;

@Getter
@RequiredArgsConstructor
public class BuildCommands {
    private static final String ENVS_DIR = "envs";

    private final ComponentTree componentTree;
    private final EnvironmentProvider environmentProvider;
    private final File destinationComponentDir;
    private final String serviceInnerDir;

    public static BuildCommands init(File repoDir, File destinationComponentDir) {
        return init(repoDir, destinationComponentDir, "");
    }

    public static BuildCommands init(File repoDir, File destinationComponentDir, String serviceInnerDir) {
        repoDir = canonical(repoDir);
        ComponentTree componentTree = ComponentTreeCache.build(repoDir);
        EnvironmentProvider environmentProvider = newEnvProvider(repoDir);
        return new BuildCommands(componentTree, environmentProvider, destinationComponentDir, serviceInnerDir);
    }

    public PropertiesProvider newPropertiesProvider(PropertyType propertyType) {
        PropertiesProvider fileBasedPropertiesProvider = cache(
                new FileBasedPropertiesProvider(componentTree, propertyType.getExtension(), new FileComponentParser(componentTree.getRepoDirRoot()))
        );
        PropertiesProvider envSpecificPropertiesProvider = cache(
                new EnvSpecificPropertiesProvider(
                        fileBasedPropertiesProvider, environmentProvider, componentTree, destinationComponentDir
                )
        );
        PropertyResolver placeholderResolver = cache(
                new SpelExpressionResolver(
                        cache(new PlaceholderResolver(environmentProvider, new PropertyFetcherImpl(envSpecificPropertiesProvider)))
                )
        );
        return cache(new ResolvedPropertiesProvider(envSpecificPropertiesProvider, placeholderResolver));
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type) {
        return newBuildCommand(type, propertySerializer(type), emptyPostProcessor());
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type, PropertySerializer propertySerializer) {
        return newBuildCommand(type, propertySerializer, emptyPostProcessor());
    }

    public BuildPropertiesCommand newBuildCommand(PropertyType type, PropertiesPostProcessor propertiesPostProcessor) {
        return newBuildCommand(type, propertySerializer(type), propertiesPostProcessor);
    }

    private BuildPropertiesCommand newBuildCommand(PropertyType type, PropertySerializer propertySerializer, PropertiesPostProcessor propertiesPostProcessor) {
        return new BuildPropertiesCommand(environmentProvider, newPropertiesProvider(type), propertySerializer, propertiesPostProcessor);
    }

    private static EnvironmentProvider newEnvProvider(File repoDir) {
        return cache(new FileBasedEnvironmentProvider(new File(repoDir, ENVS_DIR), new EnvironmentParserImpl()));
    }

    private PropertySerializer propertySerializer(PropertyType propertyType) {
        return new PropertiesDiffWriter(new PropertiesSerializerImpl(destinationComponentDir, serviceInnerDir + "/" + propertyType.getResultFile()));
    }
}