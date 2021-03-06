package org.dragon.zhang.annotator.annotator.impl;

import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import org.codehaus.plexus.logging.Logger;
import org.dragon.zhang.annotator.annotator.AbstractAnnotator;
import org.dragon.zhang.annotator.annotator.Annotator;
import org.dragon.zhang.annotator.model.JavadocMapping;
import org.jboss.forge.roaster.model.JavaDocTag;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangzicheng
 * @date 2021/03/10
 */
public class MethodAnnotator extends AbstractAnnotator {

    public MethodAnnotator(Logger log) {
        super(log);
    }

    @Override
    protected Map<String, Set<JavadocMapping>> needToTag(Set<JavadocMapping> needToTag, JavaClassSource source) {
        Map<String, Set<JavadocMapping>> result = new HashMap<>();
        List<MethodSource<JavaClassSource>> methods = source.getMethods();
        for (MethodSource<JavaClassSource> method : methods) {
            result.put(method.getName(), AbstractAnnotator.buildNeedToTag(method, needToTag));
        }
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> initComment(JavaClassSource source) {
        Map<String, Map<String, Object>> comment = new HashMap<>();
        List<MethodSource<JavaClassSource>> methods = source.getMethods();
        for (MethodSource<JavaClassSource> method : methods) {
            Map<String, Object> map = new HashMap<>();
            JavaDocSource<?> methodDoc = method.getJavaDoc();
            map.put(Annotator.DESCRIPTION_KEY, methodDoc.getText());
            List<JavaDocTag> methodTags = methodDoc.getTags();
            for (JavaDocTag tag : methodTags) {
                String tagName = tag.getName();
                if (Annotator.PARAM_TAG.equals(tagName)) {
                    continue;
                }
                map.put(tagName, tag.getValue());
            }
            comment.put(method.getName(), map);
        }
        return comment;
    }

    @Override
    protected DynamicType.Builder<?> tagAnnotations(DynamicType.Builder<?> builder, Collection<AnnotationDescription> descriptions, String name, int index) {
        return builder.visit(new MemberAttributeExtension.ForMethod()
                .annotateMethod(descriptions)
                .on(ElementMatchers.named(name)));
    }

    @Override
    public ElementType annotateType() {
        return ElementType.METHOD;
    }
}
