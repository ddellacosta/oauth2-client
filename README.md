# oauth2-client

*Not yet ready for prime-time*

Current version on [clojars](https://clojars.org/oauth2-client):

```clojure
[ddellacosta/oauth2-client "0.1.0"]
```

oauth2-client is a minimalist client library for accessing OAuth2/OpenID Connect services as a client.  It supports the Authorization Code Grant flow in the OAuth2 specification (https://tools.ietf.org/html/rfc6749).

It's intended to provide as thin of a wrapper around basic Ring functionality as possible, and uses immutable data structures and pure functions as consistent with the Ring ethos, wherever possible.

Checkout the github resource authorization example in `/examples`.

## Related Libraries

* clj-oauth2 (a bit old and suffering from bit-rot, furthermore not super functional, IMHO)
* clj-oauth (supports OAuth v1)
* friend-oauth2 (requires integration with Friend, (will use) uses this lib under the hood)
* clauth (provides server functionality, not sure if it works as a client)

## Contributing/Testing

TODO

## License

Distributed under the MIT License (http://dd.mit-license.org/)

[1] https://github.com/ddellacosta/friend-oauth2
[2] https://github.com/mattrepl/clj-oauth
[3] https://github.com/DerGuteMoritz/clj-oauth2
[4] https://github.com/pelle/clauth 
