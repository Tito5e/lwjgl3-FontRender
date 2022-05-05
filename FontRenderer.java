public class FontRenderer {

    //インスタンス化の抑制
    private FontRenderer() {}

    private static Map<String, Integer> cachedId = new HashMap<>();
    
    public static ByteBuffer getTextTexture(String text, Font font, Color fontColor) {

    //まずは描画後のテキストのサイズを取得します
    Canvas canvas = new Canvas();
    FontMetrics metrics = canvas.getFontMetrics(font);

    int width = metrics.stringWidth(text);
    int height = metrics.getHeight();
    int base = metrics.getAscent();

    //ここからBufferedImage錬成
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)

    Graphics graphics = bufferedImage.getGraphics();

    //まず背景を透明化(この工程は必要ない可能性アリ)
    graphics.setColor(new Color(0, 0, 0, 0));
    graphics.fillRect(0, 0, width, height);

    //テキストを描画
    graphics.setFont(font);
    graphics.setColor(fontColor);
    graphics.drawString(text, 0, 0+base);

    //描画の終了を宣言
    graphics.dispose();

    //チャンネル数の取得?
    int c = bufferedImage.getColorModel().getNumComponents();

    //錬成した画像の情報を取得
    byte[] data = new byte[width * height * c];
    bufferedImage.getRaster().getDataElements(0, 0, width, height, data);

    //ByteBufferに変換
    ByteBuffer pixels = BufferUtils.createByteBuffer(data.length);
    pixels.put(data);

    //ポインターを最初に戻す
    pixels.flip();

    return pixels;
}
    
    public static int registerTextTexture(String text, Font font, Color fontColor) {

    //文字列のByteBufferを取得
    ByteBuffer pixels = getTextTexture(text, font, fontColor);

    //テクスチャのIDを確保
    int id = GL11.glGenTextures();

    //今からid番のテクスチャを操作すると宣言
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

    //ここから無駄なコード
    //テクスチャの登録には幅と高さの情報が必要なので、もう一度FontMetricsを生成する
    //もし上の関数と合成させるなら削除してok
    Canvas canvas = new Canvas();
    FontMetrics metrics = canvas.getFontMetrics(font);

    int width = metrics.stringWidth(text);
    int height = metrics.getHeight();

    //テクスチャの情報を送り込む
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.RGBA, GL11.UNSIGNED_BYTE, pixels);

    //細かい設定
    //テクスチャの繰り返しを抑制する
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

    //テクスチャの補完方法の設定
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

    //ここでテクスチャの操作を終わることを宣言
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

    return id;
}

    public static void render(String text, Font font, Color fontColor, int x, int y) {

        //キャッシュの確認
        if (cachedId.containsKey(text)) {
            int id = cachedId.get(text);
        } else {
            int id = registerTextTexture(text, font, fontColor);
            cachedId.put(text, id);
        }

        //画面のサイズを取得
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);

        GLFW.glfwGetWindowSize(なんらかの方法でWindowHandleを取得する, w, h);

        int monitorWidth = w.get(0);
        int monitorHeight = h.get(0);

        //おなじみの無駄な処理
        Canvas canvas = new Canvas();
        FontMetrics metrics = canvas.getFontMetrics(font);

        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslated(x+width/2, monitorHeight-y+height/2, 0);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GL11.glOrtho(0, monitorWidth, 0, monitorHeight, -10, 10);
		
        GL11.glViewport(0, 0, monitorWidth, monitorHeight);
		
        GL11.glColor4d(1, 1, 1, 1);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2d(0, 0);
        GL11.glVertex2d(-width / 2, height / 2);
        GL11.glTexCoord2d(0, 1);
        GL11.glVertex2d(-width / 2, -height / 2);
        GL11.glTexCoord2d(1, 0);
        GL11.glVertex2d(width / 2, height / 2);
		GL11.glTexCoord2d(1, 1);
		GL11.glVertex2d(width / 2, -height / 2);
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

    }

    public static clearCache() {
        cachedId.entrySet()
			.parallelStream()
			.forEach((entry) -> {
				GL11.glDeleteTextures(entry.getValue());
			});
		
		cachedId.clear();
    }

}
