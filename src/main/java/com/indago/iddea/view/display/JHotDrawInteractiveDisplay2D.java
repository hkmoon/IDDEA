package com.indago.iddea.view.display;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

import com.indago.iddea.view.overlay.TransformOverlay;

import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.ui.InteractiveDisplayCanvas;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformEventHandlerFactory;
import net.imglib2.ui.TransformListener;

/**
 * JHotDrawInteractiveDisplay2D gives 2D display view with the buffered image.
 * This class uses MultiResolutionRenderer for realtime scaling view.
 *
 * @version 0.1beta
 * @since 8/12/13 5:09 PM
 * @author TobiasPietzsch <tobias.pietzsch@gmail.com>
 * @author HongKee Moon
 */

public class JHotDrawInteractiveDisplay2D<T> extends InteractiveDrawingView
        implements InteractiveDisplayCanvas<T>
{
    /**
     * Mouse/Keyboard handler that manipulates the view transformation.
     */
    protected TransformEventHandler< T > handler;

    /**
     * Listeners that we have to notify about view transformation changes.
     */
    final protected CopyOnWriteArrayList< TransformListener< T > > transformListeners;

    /**
     * The {@link OverlayRenderer} that draws on top of the current {@link #bufferedImage}.
     */
    final protected CopyOnWriteArrayList<OverlayRenderer> overlayRenderers;

    AffineTransform originTransform;


    /**
     * The {@link BufferedImage} that is actually drawn on the canvas. Depending
     * on {@link #discardAlpha} this is either the {@link BufferedImage}
     * obtained from {@link #screenImage}, or {@link #screenImage}s buffer
     * re-wrapped using a RGB color model.
     */
    protected BufferedImage bufferedImage;

    public JHotDrawInteractiveDisplay2D( final int width, final int height, final T sourceTransform, final TransformEventHandlerFactory< T > factory)
    {
        super();

        if(sourceTransform != null)
        {
            final AffineTransform2D trsf = (AffineTransform2D)sourceTransform;
            final double[] tr = trsf.getRowPackedCopy();
            this.originTransform = new AffineTransform(tr[0], tr[3], tr[1], tr[4], tr[2], tr[5]);
        }

        setPreferredSize( new Dimension( width, height ) );
        setFocusable( true );

        this.bufferedImage = null;
        this.overlayRenderers = new CopyOnWriteArrayList< OverlayRenderer >();
        this.transformListeners = new CopyOnWriteArrayList< TransformListener< T > >();

        addTransformListener(new TransformListener<T>(){
            @Override
            public void transformChanged(final T transform) {

                if(AffineTransform2D.class.isInstance(transform))
                {
                    // Convert AffineTransform2D to java.awt.geo.AffineTransform object
                    final AffineTransform2D trsf = (AffineTransform2D)transform;
                    // array design is different
                    final double[] tr = trsf.getRowPackedCopy();
                    preTransform = new AffineTransform(tr[0], tr[3], tr[1], tr[4], tr[2], tr[5]);
                    invalidateHandles();
                }
            }
        });

        addComponentListener( new ComponentAdapter()
        {
            @Override
            public void componentResized( final ComponentEvent e )
            {
                final int w = getWidth();
                final int h = getHeight();
                handler.setCanvasSize( w, h, true );
                for ( final OverlayRenderer or : overlayRenderers )
                    or.setCanvasSize( w, h );

                //setBounds(0,0,w,h);
//				enableEvents( AWTEvent.MOUSE_MOTION_EVENT_MASK );
            }
        } );

        addMouseListener( new MouseAdapter()
        {
            @Override
            public void mousePressed( final MouseEvent e )
            {
                requestFocusInWindow();
            }
        } );

        handler = factory.create( this );
        handler.setCanvasSize( width, height, false );

        //transformChanged(sourceTransform);
        //activateHandler will call addHandler(handler) in case of changing to DisplayTool
        //addHandler( handler );
    }

    protected Dimension imageDim;

    public void setImageDim(final Dimension dim)
    {
        imageDim = dim;
    }

    public void resetTransform()
    {
        final AffineTransform2D reset = new AffineTransform2D();
        handler.setTransform((T) reset);
        handler.setCanvasSize( imageDim.width, imageDim.height, false );

		final int w = Math.max( getWidth(), imageDim.width );
		final int h = Math.max( getHeight(), imageDim.height );

        handler.setCanvasSize( w, h, true );
        for ( final OverlayRenderer or : overlayRenderers )
            or.setCanvasSize( w, h );
    }

    public void updateTransform(final T trf)
    {
        if(trf != null)
        {
            final AffineTransform2D trsf = (AffineTransform2D)trf;
            final double[] tr = trsf.getRowPackedCopy();
            this.originTransform = new AffineTransform(tr[0], tr[3], tr[1], tr[4], tr[2], tr[5]);

            handler.setTransform((T) trsf);
            handler.setCanvasSize( imageDim.width, imageDim.height, false );

			final int w = Math.max( getWidth(), imageDim.width );
			final int h = Math.max( getHeight(), imageDim.height );

            handler.setCanvasSize( w, h, true );
            for ( final OverlayRenderer or : overlayRenderers )
                or.setCanvasSize( w, h );
        }
    }

    public void activateHandler()
    {
        addHandler( handler );
    }

    public void deactivateHandler()
    {
        removeHandler( handler );
    }

    @Override
    public void drawImage(final Graphics2D g) {
        final BufferedImage bi;
        synchronized ( this )
        {
            bi = bufferedImage;
        }

        if ( bi != null )
        {
            g.drawImage( bi, 0, 0, getWidth(), getHeight(), null );
        }

        for ( final OverlayRenderer or : overlayRenderers ) {
            if(or instanceof TransformOverlay)
            {
                ((TransformOverlay) or).setupTransform(preTransform);
            }
            or.drawOverlays(g);
        }
    }


    public Point2D.Double viewToOrigin(final Point2D.Double p) {
        Point2D point = null;

        if(originTransform != null)
        {
            try {

                point = originTransform.inverseTransform(p, null);
            } catch (final NoninvertibleTransformException e) {
                e.printStackTrace();
            }
        }

        return (Point2D.Double) point;
    }

    public Point2D.Double originToView(final double x, final double y) {
        final Point2D.Double p = new Point2D.Double(x, y);
        Point2D point = null;

        if(originTransform != null)
        {
            point = originTransform.transform(p, null);
        }

        return (Point2D.Double) point;
    }

    public Rectangle2D.Double viewToOrigin(final Rectangle2D.Double r) {
        final double[] drawing = {r.x, r.y, r.x + r.width, r.y, r.x + r.width, r.y + r.height, r.x, r.y + r.height};
        final double[] view = new double[8];

        if(originTransform != null)
        {
            try {
                originTransform.inverseTransform(drawing, 0, view, 0, 4);
            } catch (final NoninvertibleTransformException e) {
                e.printStackTrace();
            }
        }

        return new Rectangle2D.Double(view[0], view[1], view[2] - view[0], view[5] - view[1]);
    }

    public Rectangle2D.Double originToView(final Rectangle2D.Double r)
    {
        final double[] drawing = {r.x, r.y, r.x + r.width, r.y, r.x + r.width, r.y + r.height, r.x, r.y + r.height};
        final double[] view = new double[8];
        preTransform.transform(drawing, 0, view, 0, 4);

        return new Rectangle2D.Double(drawing[0], drawing[1], drawing[2] - drawing[0], drawing[5] - drawing[1]);
    }

    /**
     * Set the {@link BufferedImage} that is to be drawn on the canvas.
     *
     * @param bufferedImage image to draw (may be null).
     */
    public synchronized void setBufferedImage( final BufferedImage bufferedImage )
    {
        this.bufferedImage = bufferedImage;
    }

    /**
     * Add an {@link OverlayRenderer} that draws on top of the current {@link #bufferedImage}.
     *
     * @param renderer overlay renderer to add.
     */
    @Override
	public void addOverlayRenderer( final OverlayRenderer renderer )
    {
        overlayRenderers.add( renderer );
        renderer.setCanvasSize( getWidth(), getHeight() );
    }

    /**
     * Remove an {@link OverlayRenderer}.
     *
     * @param renderer overlay renderer to remove.
     */
    @Override
	public void removeOverlayRenderer( final OverlayRenderer renderer )
    {
        overlayRenderers.remove( renderer );
    }

    /**
     * Add a {@link TransformListener} to notify about view transformation changes.
     *
     * @param listener the transform listener to add.
     */
    @Override
	public void addTransformListener( final TransformListener< T > listener )
    {
        transformListeners.add( listener );
    }

    /**
     * Remove a {@link TransformListener}.
     *
     * @param listener the transform listener to remove.
     */
    @Override
	public void removeTransformListener( final TransformListener< T > listener )
    {
        transformListeners.remove( listener );
    }

    /**
     * Add new event handler. Depending on the interfaces implemented by
     * <code>handler</code> calls {@link Component#addKeyListener(KeyListener)},
     * {@link Component#addMouseListener(MouseListener)},
     * {@link Component#addMouseMotionListener(MouseMotionListener)},
     * {@link Component#addMouseWheelListener(MouseWheelListener)}.
     */
    @Override
	public void addHandler( final Object handler )
    {
        if ( KeyListener.class.isInstance( handler ) )
            addKeyListener( ( KeyListener ) handler );

        if ( MouseMotionListener.class.isInstance( handler ) )
            addMouseMotionListener( ( MouseMotionListener ) handler );

        if ( MouseListener.class.isInstance( handler ) )
            addMouseListener( ( MouseListener ) handler );

        if ( MouseWheelListener.class.isInstance( handler ) )
            addMouseWheelListener( ( MouseWheelListener ) handler );
    }

    /**
     * Remove an event handler.
     * Add new event handler. Depending on the interfaces implemented by
     * <code>handler</code> calls {@link Component#removeKeyListener(KeyListener)},
     * {@link Component#removeMouseListener(MouseListener)},
     * {@link Component#removeMouseMotionListener(MouseMotionListener)},
     * {@link Component#removeMouseWheelListener(MouseWheelListener)}.
     */
    @Override
	public void removeHandler( final Object handler )
    {
        if ( KeyListener.class.isInstance( handler ) )
            removeKeyListener( ( KeyListener ) handler );

        if ( MouseMotionListener.class.isInstance( handler ) )
            removeMouseMotionListener( ( MouseMotionListener ) handler );

        if ( MouseListener.class.isInstance( handler ) )
            removeMouseListener( ( MouseListener ) handler );

        if ( MouseWheelListener.class.isInstance( handler ) )
            removeMouseWheelListener( ( MouseWheelListener ) handler );
    }

    /**
     * Get the {@link TransformEventHandler} that handles mouse and key events
     * to update our view transform.
     *
     * @return handles mouse and key events to update the view transform.
     */
    @Override
	public TransformEventHandler< T > getTransformEventHandler()
    {
        return handler;
    }

    /**
     * Set the {@link TransformEventHandler} that handles mouse and key events
     * to update our view transform.
     *
     * @param transformEventHandler mouse and key events to update the view transform
     */
    @Override
	public synchronized void setTransformEventHandler( final TransformEventHandler< T > transformEventHandler )
    {
        removeHandler( handler );
        handler = transformEventHandler;
        handler.setCanvasSize( getWidth(), getHeight(), false );
        addHandler( handler );
    }

    /**
     * This is called by our {@link #getTransformEventHandler() transform event
     * handler} when the transform is changed. In turn, we notify all our
     * {@link TransformListener TransformListeners} that the view transform has
     * changed.
     */
    @Override
    public void transformChanged( final T transform )
    {
        for ( final TransformListener< T > l : transformListeners )
            l.transformChanged( transform );
    }

    @Override
    public void setPreferredSize( final Dimension dim )
    {
        super.setPreferredSize(dim);
        setImageDim(dim);
    }
}
