# ICU Support Grails Plugin

Provides the [ICU4J](http://site.icu-project.org/) message formatting features, such as named arguments support, flexible plural formatting,
rule based number format, date interval formats.

## Features
### Named arguments
By default Grails allows to use only numbered arguments in i18n messages. The plugin allows to use also named arguments,
which sometimes are more readable. For example:
```
numbered={0}, you have {1} unread messages of {2}
names={username}, you have {unread} unread messages of {total}
```
With the plugin, latter message can be used:
```html
<g:message code="names" args="[username: 'John', unread: 12, total: 200]"/>
```
will output `John, you have 12 unread messages of 200`.
### Plural formatting
Pluralisation in English is pretty simple and can be implemented using embedded `ChoiceFormat`. However, many other
languages have more complex pluralisation rules [described here](http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html),
which cannot be handled by default. The plugin provides a simple pluralization using a language's rules, e.g. for Polish:
```
plural={0} {0, plural, one{auto}few{auta}many{aut}other{aut}}
```
```html
<g:message code="plural" args="[3]"/>, <g:message code="plural" args="[7]"/>
```
will output `3 auta, 7 aut`.
### Rule based number formatting
```
amount={0, spellout} dollars
```
```html
<g:message code="amount" args="[12045]"/>
```
will output `twelve thousand forty-five dollars`.
### Other features
- ICU implements a more user-friendly apostrophe quoting syntax. In message text, an apostrophe only begins quoting
literal text if it immediately precedes a syntax character (mostly {curly braces}). By default an apostrophe always
begins quoting, which requires common text like "don't" and "aujourd'hui" to be written with doubled apostrophes like "don''t" and "aujourd''hui".
- Many more date formats: month+day, year+month,...
- Date interval formats: "Dec 15-17, 2009"

# License
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

(c) All rights reserved