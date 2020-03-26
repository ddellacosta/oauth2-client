# This project is unsupported. Fork at will.

# oauth2-client

Current version on [clojars](https://clojars.org/oauth2-client):

```clojure
[ddellacosta/oauth2-client "0.1.0"]
```

oauth2-client is a minimalist client library for accessing OAuth2/OpenID Connect services as a client.  It supports the Authorization Code Grant flow in the OAuth2 specification (https://tools.ietf.org/html/rfc6749).

It's intended to provide as thin of a wrapper around basic Ring functionality as possible, be easily extendable, modular so you can take what you want and leave the rest, and as idiomatically-written Clojure as possible.  Includes protection against CSRF (per OAuth2 RFC: https://tools.ietf.org/html/rfc6749#section-10.12) "on" by default.

Checkout the Github and Google examples in `/examples`.

## API and Documentation

* [API](http://ddellacosta.github.io/oauth2-client)
* Wiki *TODO*

## Other Related Libraries

There is certainly overlap between this library and many others out there.  Here's a list below so you can compare and contrast, with some notes added where appropriate.

* [clj-oauth2](https://github.com/DerGuteMoritz/clj-oauth2) - OAuth2 client
* [oauthentic](https://github.com/pelle/oauthentic) - OAuth2 client
* [oauth-clj](https://github.com/r0man/oauth-clj) - supports OAuth v1 and v2 apparently - seems more up-to-date than some of the other libs
* [clj-oauth](https://github.com/mattrepl/clj-oauth) - supports OAuth v1
* [friend-oauth2](https://github.com/ddellacosta/friend-oauth2) - for integration with Friend, should technically be considered a OpenID connect implementation, no longer actively maintained
* [clauth](https://github.com/pelle/clauth) - go-to for implementing a provider, don't believe it works as a client
* [qarth](https://github.com/mthvedt/qarth) - more comprehensive library for doing Friend-based OAuth2 as well as sans-Friend OAuth2 requests. Offers a bunch of pre-defined helpers for the most popular services like Google, FB, etc.

## Contributing/Testing/Etc.

TODO

## License

Distributed under the MIT License (http://dd.mit-license.org/)

[1] https://github.com/DerGuteMoritz/clj-oauth2  
[2] https://github.com/pelle/oauthentic  
[3] https://github.com/r0man/oauth-clj  
[4] https://github.com/mattrepl/clj-oauth  
[5] https://github.com/ddellacosta/friend-oauth2  
[6] https://github.com/pelle/clauth  
[7] https://github.com/mthvedt/qarth  
