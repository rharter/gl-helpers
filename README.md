# GL Helpers

A collection of helper classes to make working with OpenGL on
Android easier.

## GLState

Keeps track of the current state of the OpenGL context to avoid
superfluous uploads.

## Textures

Texture and it's subclasses help with binding, image mapping, and
other texture operations.

## Programs

Eases working with Shader programs in OpenGL. Easily compile and
link programs and access their uniforms and attributes.

## Exporting

To export the current GL state, you simply need to create a
`WriteableTexture` of the size you want the exported image,
bind the framebuffer, render the state, then call `getBitmap()`
on the `WriteableTexture`.

```java
final int width = renderer.width;
final int height = renderer.height;

WritableTexture exportTexture = new WritableTexture(4096, 4096);
exportTexture.bindFramebuffer();

renderer.onSurfaceChanged(null, 4096, 4096);
renderer.render(true);

Bitmap bitmap = exportTexture.getBitmap();
writeBitmap("read_pixels", bitmap);

exportTexture.unbindFramebuffer();
renderer.onSurfaceChanged(null, width, height);
```