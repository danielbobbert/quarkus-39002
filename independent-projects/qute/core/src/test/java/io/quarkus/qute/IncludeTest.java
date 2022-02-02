package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class IncludeTest {

    @Test
    public void testInclude() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super", engine.parse("{this}: {#insert header}default header{/insert}"));
        assertEquals("HEADER: super header",
                engine.parse("{#include super}{#header}super header{/header}{/include}").render("HEADER"));
    }

    @Test
    public void testMultipleInserts() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super",
                engine.parse("{#insert header}default header{/insert} AND {#insert content}default content{/insert}"));

        Template template = engine
                .parse("{#include super}{#header}super header{/header}  {#content}super content{/content} {/include}");
        assertEquals("super header AND super content", template.render(null));
    }

    @Test
    public void testIncludeSimpleData() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("name", "Al");
        data.put("price", "100");
        engine.putTemplate("detail", engine.parse("<strong>{name}</strong>:{price}"));
        assertEquals("<strong>Al</strong>:100",
                engine.parse("{#include detail/}").render(data));
    }

    @Test
    public void testOptionalBlockEndTags() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}header{/}:{#insert footer /}"));
        assertEquals("super header:super footer",
                engine.parse("{#include super}{#header}super header{#footer}super footer{/include}").render());
    }

    @Test
    public void testIncludeInLoop() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1.2.3.4.5.",
                engine.parse(
                        "{#for i in 5}{#include foo}{#snippet}{i_count}.{/snippet} this should be ingored {/include}{/for}")
                        .render());
    }

    @Test
    public void testIncludeInIf() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1",
                engine.parse("{#if true}{#include foo} {#snippet}1{/snippet} {/include}{/if}")
                        .render());
    }

    @Test
    public void testUserTagInsideInsert() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new UserTagSectionHelper.Factory("hello", "hello"))
                .build();
        engine.putTemplate("hello", engine.parse("{name}"));
        engine.putTemplate("base", engine.parse("{#insert snippet}{/insert}"));
        assertEquals("foo",
                engine.parse("{#include base} {#snippet}{#hello name='foo'/}{/snippet} {/include}")
                        .render());
    }

    @Test
    public void testIncludeStandaloneLines() {
        Engine engine = Engine.builder().addDefaults().removeStandaloneLines(true).build();
        engine.putTemplate("super", engine.parse("{#insert header}\n"
                + "default header\n"
                + "{/insert}"));
        assertEquals("super header\n",
                engine.parse("{#include super}\n"
                        + "{#header}\n"
                        + "super header\n"
                        + "{/header}\n"
                        + "{/include}").render());
    }

    @Test
    public void testEmptyInclude() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("bar/fool.html", engine.parse("{foo} and {that}"));
        assertEquals("1 and true", engine.parse("{#include bar/fool.html that=true /}").data("foo", 1).render());
    }

    @Test
    public void testInsertParam() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}default header{/insert} and {#insert footer}{that}{/}"));
        Template foo = engine.parse("{#include 'super' that=foo}{#header}{that}{/}{/}");
        // foo, that
        assertEquals(2, foo.getExpressions().size());
        assertEquals("1 and 1", foo.data("foo", 1).render());
    }

    @Test
    public void testDefaultInsert() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>{#insert title}Default Title{/}</title>"
                + "</head>"
                + "<body>"
                + "  {#insert}No body!{/}"
                + "</body>"
                + "</html>"));
        assertEquals("<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>My Title</title>"
                + "</head>"
                + "<body>"
                + "  Body 1!"
                + "</body>"
                + "</html>", engine.parse("{#include super}{#title}My Title{/title}Body {foo}!{/}").data("foo", 1).render());
    }

    @Test
    public void testAmbiguousInserts() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}default header{/insert}"));
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#include super}{#header}1{/}{#header}2{/}{/}"))
                .withMessage(
                        "Multiple blocks define the content for the {#insert} section of name [header] on line 1")
                .hasFieldOrProperty("origin");
    }

    @Test
    public void testInsertInLoop() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#for i in 5}{#insert row}No row{/}{/for}"));
        assertEquals("1:2:3:4:5:", engine.parse("{#include super}{#row}{i}:{/row}{/}").render());
    }

    @Test
    public void testTagAndInsertConflict() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new UserTagSectionHelper.Factory("row", "row")).build();
        engine.putTemplate("row", engine.parse("{foo}"));
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#insert}{/}\n{#insert row /}"))
                .withMessage(
                        "An {#insert} section defined in the {#include} section on line 2 conflicts with an existing section/tag: row")
                .hasFieldOrProperty("origin");
    }

}
