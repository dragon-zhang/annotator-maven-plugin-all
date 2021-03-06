package org.dragon.zhang.annotator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.dragon.zhang.annotator.annotator.Annotator;
import org.dragon.zhang.annotator.annotator.impl.ClassAnnotator;
import org.dragon.zhang.annotator.annotator.impl.FieldAnnotator;
import org.dragon.zhang.annotator.annotator.impl.MethodAnnotator;
import org.dragon.zhang.annotator.annotator.impl.ParameterAnnotator;
import org.dragon.zhang.annotator.model.AnnotatorConfig;
import org.dragon.zhang.annotator.model.JavadocMapping;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangzicheng
 * @date 2021/03/10
 */
@Component(role = ModelBuilder.class)
@Mojo(name = "annotate", defaultPhase = LifecyclePhase.COMPILE)
public class AnnotatorMojo extends AbstractMojo {

    @Requirement
    protected Logger log;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * 匹配的类型需要打什么注解
     */
    @Parameter(property = "configs", required = true)
    protected Set<AnnotatorConfig> configs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("annotator-maven-plugin started !!!");
        String sourceDirectory = project.getBuild().getSourceDirectory();
        log.info("sourceDirectory->" + sourceDirectory);
        String outputDirectory = project.getBuild().getOutputDirectory();
        log.info("outputDirectory->" + outputDirectory);
        if (CollectionUtils.isEmpty(configs)) {
            log.error("configs is empty !");
            return;
        }
        Map<ElementType, Set<JavadocMapping>> config = new HashMap<>(16);
        for (AnnotatorConfig annotatorConfig : configs) {
            ElementType type = annotatorConfig.getAnnotateType();
            Set<JavadocMapping> set = config.get(type);
            if (null == set) {
                set = new HashSet<>();
            }
            set.addAll(annotatorConfig.getJavadocMappings());
            config.put(type, set);
        }
        log.info("configs->" + JSON.toJSONString(config, SerializerFeature.PrettyFormat,
                SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat));
        List<Annotator> annotators = getSupportedAnnotators();
        for (File javaFile : getJavaFiles(sourceDirectory)) {
            try {
                JavaClassSource source = Roaster.parse(JavaClassSource.class, javaFile);
                DynamicType.Builder<?> builder = new ByteBuddy().redefine(Class.forName(source.getQualifiedName()));
                for (Annotator annotator : annotators) {
                    builder = annotator.annotate(builder, config.get(annotator.annotateType()), source);
                }
                builder.make().saveIn(new File(outputDirectory));
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
            }
        }
        log.info("annotator-maven-plugin finished !!!");
    }

    protected List<Annotator> getSupportedAnnotators() {
        List<Annotator> annotators = new LinkedList<>();
        annotators.add(new ClassAnnotator(this.log));
        annotators.add(new FieldAnnotator(this.log));
        annotators.add(new MethodAnnotator(this.log));
        annotators.add(new ParameterAnnotator(this.log));
        return annotators;
    }

    public Set<File> getJavaFiles(String rootPath) {
        Set<File> allJavaFiles = new LinkedHashSet<>();
        // 设置java文件所在根路径
        File rootDir = new File(rootPath);
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        allJavaFiles.addAll(getJavaFiles(file.getAbsolutePath()));
                    } else {
                        allJavaFiles.add(file);
                    }
                }
            }
        }
        return allJavaFiles;
    }
}
