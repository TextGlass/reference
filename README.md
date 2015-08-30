TextGlass Reference
===================

What is textglass?
------------------

TextGlass is primarily a text classification project. This encompasses
things like device detection. Basically, give TextGlass a string (like
a User-Agent request header) and it will tell you what it is. 

TextGlass has extended the idea of classification into generic domains.
The domain tells the TextGlass client how to classify the input into a
result. This will allow this project and 3rd parties to create their own
domains and use them with standard TextGlass clients.

This reference is the starting point for:

 * Creating, testing, and validating a TextGlass domain.
 * Creating a TextGlass client for a specific platform or language.

Specification
-------------

https://github.com/TextGlass/reference/wiki/Domain-Specification

More information
----------------

Please see [client/README.md](client/README.md) for more information.