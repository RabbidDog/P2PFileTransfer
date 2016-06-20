package pft.frames;

public class DataRequest extends Frame {

  private long offset;
  private long length;
  private String fileName;
  private byte[] sha;

  public DataRequest(int identifier, long offset, long length) {
    super(identifier);
    this.offset = offset;
    this.length = length;
  }
  public DataRequest(int identifier,String fileName, byte[] sha, long offset, long length) {
    super(identifier);
    this.offset = offset;
    this.length = length;
    this.fileName = fileName;
    this.sha = sha;
  }

  @Override public byte type() {
    return 5;
  }

  public long offset() {
    return offset;
  }

  public long length() {
    return length;
  }
   public String fileName(){
     return fileName;
   }
  public byte[] sha(){
    return sha;
  }
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DataRequest)) {
      return false;
    }

    DataRequest that = (DataRequest) other;

    return that.identifier() == identifier() &&
        that.offset == offset
        && that.length == length;
  }

  @Override
  public int hashCode() {
    return identifier() + ((int) offset) + 31 * ((int) length);
  }
}
