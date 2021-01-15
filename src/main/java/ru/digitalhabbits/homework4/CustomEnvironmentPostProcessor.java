package ru.digitalhabbits.homework4;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();

    private static final String PATH = "config/";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Stream.concat(getResources().stream(), getResourcesFromDir().stream())
                .sorted(Comparator.comparing(Resource::getFilename))
                .map(this::loadProperty)
                .forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
    }

    private List<Resource> getResources() {

        final String locationPattern = String.format("classpath:%s*.properties", PATH);
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            return Arrays.stream(resolver.getResources(locationPattern))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load properties configuration from resources", e);
        }
    }

    private List<Resource> getResourcesFromDir() {

        File dir = new File(PATH);

        if (!dir.exists()) {
            return Collections.emptyList();
        }

        return Arrays.stream(
                Objects.requireNonNull(
                        dir.listFiles((d, name) -> name.endsWith(".properties"))))
                .filter(File::isFile)
                .map(FileSystemResource::new)
                .collect(Collectors.toList());
    }

    private PropertySource<?> loadProperty(Resource path) {
        if (!path.exists()) {
            throw new IllegalArgumentException("Resource " + path + " does not exist");
        }
        try {
            return this.loader.load(path.getFilename(), path).get(0);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load property configuration from " + path, ex);
        }
    }
}
