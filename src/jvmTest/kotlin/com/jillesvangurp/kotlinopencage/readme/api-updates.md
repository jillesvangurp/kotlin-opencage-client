## API coverage reviewed November 2026

- `GeosearchClient` supports place and postcode suggestions, bounds, country filters, language, proximity, and type inclusion/exclusion. It uses a separate geosearch key and endpoint. See the [official geosearch client documentation](https://github.com/OpenCageData/geosearch), [proximity update](https://blog.opencagedata.com/post/geosearch-improvements), and [type-filtering update](https://blog.opencagedata.com/post/geosearch-with-type-filtering).
- Geocoding accepts [DIGIPIN queries](https://blog.opencagedata.com/post/digipin-support) through `geocode(q = "32C-849-5CKC")`. Read the identifier from `components.digipin` or `annotations.digipin`.
- `annotations.roadInfo` exposes [the extended road fields](https://blog.opencagedata.com/post/more-roadinfo-maxspeed-data), including bus/HGV speed limits, conditional and variable limits, minimum speeds, and tunnel information. Speeds and dimensions are strings so numeric and textual values are preserved. Road data is crowdsourced and may be incomplete or outdated.
- `annotations.nuts` exposes NUTS region codes. [OpenCage's dataset update](https://blog.opencagedata.com/post/nuts-2024-data) requires no new request parameter.
- Geocoding now has a `pretty` option, typed response `status`, and access to the optional `request` metadata returned with `addRequest = true`.

The [current geocoding API](https://opencagedata.com/api) still uses `/geocode/v1/json`. UN/LOCODE queries such as `LOCODE:DEBRV` already work through `geocode`. Annotations and the complete response remain accessible as JSON for fields added in the future. OpenCage's spreadsheet-upload service is documented as a dashboard feature, so this client does not invent an upload endpoint.

### Compatibility notes

`response.rate` is now nullable: subscription responses do not contain rate limits. Use `response.rate?.remaining`. `components.type` now reads the API's `_type` field, and `components.borough` replaces the misspelled `borrow` property, which remains as a deprecated alias. Additional address fields include town, village, municipality, county, city/state district, and road type.

The new `pretty` argument is appended after `additionalRequestParams`, preserving existing positional source calls. Recompile applications against this release because public method and data-class signatures have changed.
