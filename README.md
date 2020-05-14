# kvstore

A Clojure key-value store library.

## Usage

```clojure
(require '[kvstore.core :refer [make-store store-close!
                                store-get store-put! store-delete!]])
```

Please see the Marginalia-generated
[documentation](https://poseur.com/kvstore-docs.html) for more
information.

## License

Copyright © 2020 Edwin Watkeys

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
