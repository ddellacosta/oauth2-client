# oauth2-client

*Not yet ready for prime-time*

Current version on [clojars](https://clojars.org/oauth2-client):

```clojure
[ddellacosta/oauth2-client "0.1.0"]
```

oauth2-client is a minimalist client library for accessing OAuth2/OpenID Connect services as a client.  It supports the Authorization Code Grant flow in the OAuth2 specification (https://tools.ietf.org/html/rfc6749).

It's intended to provide as thin of a wrapper around basic Ring functionality as possible, and uses immutable data structures and pure functions as consistent with the Ring ethos, wherever possible. Includes protection against CSRF (per OAuth2 RFC: https://tools.ietf.org/html/rfc6749#section-10.12) "on" by default.

Checkout the github resource authorization example in `/examples`.

## Related Libraries

* clj-oauth2 (a bit old and suffering from bit-rot, furthermore not super functional, IMHO)
* oauthentic (didn't know this existed! A bit old as well, and also a bit non-idiomatic)
* oauth-clj (supports OAuth v1 and v2 apparently - think this is more idiomatic Clojure than other libs, and also somewhat more up-to-date)
* clj-oauth (supports OAuth v1)
* friend-oauth2 (requires integration with Friend, (will use) uses this lib under the hood)
* clauth (go-to for implementing a provider, not sure if it works as a client)

Probably more I'm missing!

## Contributing/Testing

TODO

## License

Distributed under the MIT License (http://dd.mit-license.org/)

[1] https://github.com/ddellacosta/friend-oauth2
[2] https://github.com/pelle/oauthentic
[3] https://github.com/r0man/oauth-clj
[4] https://github.com/mattrepl/clj-oauth
[5] https://github.com/DerGuteMoritz/clj-oauth2
[6] https://github.com/pelle/clauth 
