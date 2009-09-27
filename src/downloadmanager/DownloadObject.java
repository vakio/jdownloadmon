package downloadmanager;

import downloadmanager.states.ActiveState;
import downloadmanager.states.StatusState;
import downloadmanager.states.ErrorState;
import downloadmanager.states.InactiveState;
import downloadmanager.events.DownloadStatusStateEvent;
import downloadmanager.events.DownloadProgressEvent;
import java.util.ArrayList;

/**
 * A download object represents anything that is being downloaded.
 * The class contains methods for connecting to and downloading files among other things.
 * @author Edward Larsson (edward.larsson@gmx.com)
 */
public class DownloadObject implements Runnable, DownloadObservable {

	/** Buffer size. */
	private static final int BUFFER_SIZE = 1024;
	/** Default directory location */
	private static final String DEFAULT_DIRECTORY = "C:/Downloads/";
	/** List of progress observers observing this download object. */
	private ArrayList<DownloadObserver> mObservers;
	/** The download file's destionation. */
	private String mDestination;
	/** The current downloaded size, in bytes. */
	private long mDownloadedSize;
	/** The size of the complete file. */
	private long mSize;
	/** The current state of this download object. */
	private StatusState mStatusState;
	/** The connection to the server. */
	private DownloadConnection mDownloadConnection;
	/** File handler to write and read files on system. */
	private FileHandler mFileHandler;

	/**
	 * Construct a download object.
	 * @param destination Where to put the file on the drive.
	 * @param connection The download connection to use for downloading.
	 */
	public DownloadObject(DownloadConnection connection) {
		mDownloadConnection = connection;
		mObservers = new ArrayList<DownloadObserver>();
		mStatusState = new InactiveState(this);
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {
		try {
			mSize = mDownloadConnection.connect(mDownloadedSize);
			mFileHandler = new FileHandler(mDownloadConnection.getURL(), mDownloadedSize);
			while (mStatusState instanceof ActiveState) {
				/**byte[] bytes = mDownloadConnection.getBytes(mDownloadedSize, mSize, BUFFER_SIZE);
				mFileHandler.write(bytes);
				mDownloadedSize += bytes.length;*/
				mFileHandler.writeByte(mDownloadConnection.getSingleByte());
				mDownloadedSize++;
				notifyListeners(new DownloadProgressEvent(this));
				}
		} catch (Exception ex) {
			setStatusState(new ErrorState(this, ex.getMessage()));
			ex.printStackTrace();
		} finally {
			mFileHandler.close();
			mDownloadConnection.close();
		}
	}

	/**
	 * Get the current state of the download object.
	 * @return the {@link StatusState} that this object is currently in.
	 */
	public StatusState getStatusState() {
		return mStatusState;
	}

	/**
	 * Get the current download percentage.
	 * @return an <tt>integer</tt> value representing how far this download has gotten percentually.
	 */
	public int getPercentDownloaded() {
		return (int) (mDownloadedSize * 100 / mSize);
	}

	/**
	 * Change the state of the download object.
	 * @param state The StatusState to change to.
	 */
	public void setStatusState(StatusState state) {
		if (mStatusState.changeTo(state)) {
			DownloadStatusStateEvent event = new DownloadStatusStateEvent(state);
			notifyListeners(event);
			mStatusState = state;
		} 
	}

	/**
	 * Get the destination.
	 * @return Destination file path, as a String.
	 */
	public String getDestination() {
		return mDestination;
	}

	/**
	 * Get the download connection of this download object.
	 * @return The download connection of this download object.
	 */
	public DownloadConnection getConnection() {
		return mDownloadConnection;
	}

	public void addListener(DownloadObserver observer) {
		mObservers.add(observer);
	}

	public void removeListener(DownloadObserver observer) {
		mObservers.remove(observer);
	}

	public void notifyListeners(DownloadProgressEvent downloadProgressEvent) {
		for (DownloadObserver observer : mObservers) {
			observer.downloadEventPerformed(downloadProgressEvent);
		}
	}

	public void notifyListeners(DownloadStatusStateEvent downloadStatusStateEvent) {
		for (DownloadObserver observer : mObservers) {
			observer.downloadEventPerformed(downloadStatusStateEvent);
		}
	}

	public void stop() {
		mStatusState.stop();
	}

	public void download() {
		mStatusState.download();
	}
}
